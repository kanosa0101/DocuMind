package com.javaee.common.config.security;

import com.javaee.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
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
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 用于验证请求中的JWT令牌并设置认证上下文
 * 支持两种认证方式：
 * 1. 网关传递的请求头（X-User-Id, X-Username, X-Role）
 * 2. 直接携带的JWT令牌（Authorization头）
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 网关传递的用户信息请求头
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_ROLE = "X-Role";

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
            // 优先从网关传递的请求头中获取用户信息（网关已验证JWT）
            String userIdHeader = request.getHeader(HEADER_USER_ID);
            String usernameHeader = request.getHeader(HEADER_USERNAME);
            String roleHeader = request.getHeader(HEADER_ROLE);

            if (userIdHeader != null && !userIdHeader.isEmpty()) {
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
}