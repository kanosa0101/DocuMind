package com.javaee.common.config.security;

import com.javaee.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

/**
 * JWT认证过滤器
 * 用于验证请求中的JWT令牌并设置认证上下文
 * 支持两种认证方式：
 * 1. 网关传递的请求头（X-User-Id, X-Username, X-Role）- 需要签名验证
 * 2. 直接携带的JWT令牌（Authorization头）
 * 3. 内部服务调用（FeignClient）- 跳过签名验证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 网关传递的用户信息请求头
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_ROLE = "X-Role";
    private static final String HEADER_REQUEST_SIGNATURE = "X-Request-Signature";
    private static final String HEADER_REQUEST_TIMESTAMP = "X-Request-Timestamp";

    // 签名验证时间窗口（5分钟）
    private static final long SIGNATURE_TIME_WINDOW_MS = 300000;

    // 内部服务调用路径白名单（跳过签名验证）
    private static final Set<String> INTERNAL_SERVICE_PATHS = Set.of(
        "/api/ai/summarize",
        "/api/ai/keywords",
        "/api/ai/classify",
        "/api/ai/analyze",
        "/api/ai/rag/index",
        "/api/ai/rag/delete",
        "/api/ai/rag/documents",
        "/api/ai/rag/search"
    );

    @Value("${gateway.signature.secret:}")
    private String gatewaySignatureSecret;

    private JwtUtils jwtUtils;

    /**
     * 构造函数注入JwtUtils
     * @param jwtUtils JWT工具类
     */
    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /**
     * 无参构造函数（兼容旧代码）
     */
    public JwtAuthenticationFilter() {
        // 默认构造函数，用于某些服务不需要注入JwtUtils的场景（如信任网关传递的请求头）
    }

    /**
     * 设置JwtUtils（用于手动注入）
     * @param jwtUtils JWT工具类
     */
    public void setJwtUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();

            // 优先从网关传递的请求头中获取用户信息（网关已验证JWT）
            String userIdHeader = request.getHeader(HEADER_USER_ID);
            String usernameHeader = request.getHeader(HEADER_USERNAME);
            String roleHeader = request.getHeader(HEADER_ROLE);
            String signatureHeader = request.getHeader(HEADER_REQUEST_SIGNATURE);
            String timestampHeader = request.getHeader(HEADER_REQUEST_TIMESTAMP);

            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                // 检查是否为内部服务调用路径（跳过签名验证）
                boolean isInternalCall = INTERNAL_SERVICE_PATHS.contains(requestPath);

                // 内部服务调用或有签名密钥配置时才验证签名
                if (!isInternalCall && gatewaySignatureSecret != null && !gatewaySignatureSecret.isEmpty()) {
                    // 验证签名
                    if (!validateGatewaySignature(userIdHeader, usernameHeader, signatureHeader, timestampHeader)) {
                        logger.warn("网关请求签名验证失败: userId=" + userIdHeader);
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway signature");
                        return;
                    }

                    // 验证时间戳（防止重放攻击）
                    if (!validateTimestamp(timestampHeader)) {
                        logger.warn("请求时间戳过期或无效: timestamp=" + timestampHeader);
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request timestamp expired");
                        return;
                    }

                    logger.debug("网关签名验证通过: userId=" + userIdHeader);
                } else if (isInternalCall) {
                    logger.debug("内部服务调用，跳过签名验证: path=" + requestPath + ", userId=" + userIdHeader);
                }

                // 网关已验证JWT，直接使用传递的用户信息
                Long userId = Long.parseLong(userIdHeader);
                String username = usernameHeader != null ? usernameHeader : "";
                String role = roleHeader != null ? roleHeader : "USER";

                // 创建认证令牌
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                // 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("使用网关传递的用户信息完成认证: userId=" + userId + ", username=" + username);
            } else {
                // 没有网关传递的请求头，尝试从Authorization头解析JWT（用于直连服务的场景）
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // 提取令牌
                    String token = authHeader.substring("Bearer ".length());

                    // 验证令牌
                    if (jwtUtils != null && jwtUtils.validateToken(token)) {
                        // 解析令牌获取用户信息
                        Claims claims = jwtUtils.parseToken(token);
                        Long userId = claims.get("userId", Long.class);
                        String username = claims.get("username", String.class);
                        String role = claims.get("role", String.class);

                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                        // 设置认证详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        logger.debug("使用JWT令牌完成认证: userId=" + userId + ", username=" + username);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // X-User-Id解析失败
            logger.warn("X-User-Id请求头格式错误: " + e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            // 令牌验证失败，清除认证上下文
            logger.warn("认证处理失败: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 继续过滤链
        chain.doFilter(request, response);
    }

    /**
     * 验证网关签名
     */
    private boolean validateGatewaySignature(String userId, String username, String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            logger.warn("缺少签名或时间戳头");
            return false;
        }
        String expectedSignature = calculateSignature(userId, username, timestamp);
        return expectedSignature.equals(signature);
    }

    /**
     * 计算期望签名
     */
    private String calculateSignature(String userId, String username, String timestamp) {
        String data = userId + "|" + username + "|" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                gatewaySignatureSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("签名计算失败", e);
            return "";
        }
    }

    /**
     * 验证时间戳（防重放攻击）
     */
    private boolean validateTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isEmpty()) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(timestampHeader);
            long currentTime = System.currentTimeMillis();
            // 允许5分钟的时间窗口
            return Math.abs(currentTime - timestamp) < SIGNATURE_TIME_WINDOW_MS;
        } catch (NumberFormatException e) {
            logger.warn("时间戳格式错误: " + timestampHeader);
            return false;
        }
    }
}