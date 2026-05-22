package com.javaee.gateway.filter;

import com.javaee.common.util.RabbitMQUtil;
import com.javaee.gateway.config.JwtConfig;
import com.javaee.gateway.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT鉴权全局过滤器
 * 功能：
 * 1. JWT令牌验证
 * 2. 用户信息传递到下游服务
 * 3. 请求签名生成（防止请求伪造）
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private RabbitMQUtil rabbitMQUtil;

    @Autowired
    private JwtConfig jwtConfig;

    @Value("${gateway.signature.secret}")
    private String gatewaySignatureSecret;

    // 不需要鉴权的路径（移除了过于宽松的通配符）
    private static final List<String> WHITE_LIST = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/refresh",
            "/ws/progress"  // WebSocket端点
    );

    // 签名请求头
    private static final String HEADER_REQUEST_SIGNATURE = "X-Request-Signature";
    private static final String HEADER_REQUEST_TIMESTAMP = "X-Request-Timestamp";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        log.info("网关收到请求: {} {}", method, path);

        // 发送网关日志消息
        sendGatewayLog(path, method, null);

        // 检查是否在白名单中
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 从请求头中获取令牌
        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求未授权: {} {}", method, path);
            sendGatewayAlert("UNAUTHORIZED", "缺少有效的认证令牌: " + path);
            return unauthorized(exchange);
        }

        // 提取令牌
        String token = authHeader.substring(7);

        // 验证令牌
        if (!jwtConfig.validateToken(token)) {
            log.warn("令牌无效: {} {}", method, path);
            sendGatewayAlert("INVALID_TOKEN", "无效的访问令牌: " + path);
            return unauthorized(exchange);
        }

        // 令牌有效，将用户信息存储到请求头中
        try {
            Long userId = jwtConfig.getUserId(token);
            String username = jwtConfig.getUsername(token);
            String role = jwtConfig.getRole(token);

            // 生成请求签名和时间戳（防止请求伪造）
            long timestamp = System.currentTimeMillis();
            String signature = generateRequestSignature(userId.toString(), username, timestamp);

            // 创建新的请求头
            ServerHttpRequest.Builder requestBuilder = request.mutate();
            requestBuilder.header("X-User-Id", userId.toString());
            requestBuilder.header("X-Username", username);
            requestBuilder.header(HEADER_REQUEST_SIGNATURE, signature);
            requestBuilder.header(HEADER_REQUEST_TIMESTAMP, String.valueOf(timestamp));
            if (role != null) {
                requestBuilder.header("X-Role", role);
            }

            // 更新网关日志，添加用户ID
            sendGatewayLog(path, method, userId.toString());

            log.info("请求签名已生成: userId={}, timestamp={}", userId, timestamp);

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        } catch (Exception e) {
            log.error("处理令牌时出错", e);
            sendGatewayAlert("TOKEN_ERROR", "处理访问令牌时出错: " + e.getMessage());
            return unauthorized(exchange);
        }
    }

    /**
     * 生成请求签名
     * 签名算法: HMAC-SHA256(userId + "|" + username + "|" + timestamp + "|" + secret)
     */
    private String generateRequestSignature(String userId, String username, long timestamp) {
        String data = userId + "|" + username + "|" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                gatewaySignatureSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("签名生成失败", e);
            throw new RuntimeException("签名生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送网关日志消息
     */
    private void sendGatewayLog(String path, String method, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("path", path);
        message.put("method", method);
        message.put("userId", userId);
        message.put("timestamp", LocalDateTime.now().toString());
        
        log.info("发送网关日志消息到 RabbitMQ");
        rabbitMQUtil.send(RabbitMQConfig.GATEWAY_EXCHANGE, RabbitMQConfig.GATEWAY_LOG_ROUTING_KEY, message);
    }

    /**
     * 发送网关告警消息
     */
    private void sendGatewayAlert(String alertType, String description) {
        Map<String, Object> message = new HashMap<>();
        message.put("alertType", alertType);
        message.put("description", description);
        message.put("timestamp", LocalDateTime.now().toString());
        
        log.info("发送网关告警消息到 RabbitMQ");
        rabbitMQUtil.send(RabbitMQConfig.GATEWAY_EXCHANGE, RabbitMQConfig.GATEWAY_ALERT_ROUTING_KEY, message);
    }

    @Override
    public int getOrder() {
        // 过滤器执行顺序，数字越小优先级越高
        return -100;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        for (String whitePath : WHITE_LIST) {
            if (whitePath.endsWith("**")) {
                // 支持通配符匹配，如/api/users/**
                String prefix = whitePath.substring(0, whitePath.length() - 2);
                if (path.equals(prefix) || path.startsWith(prefix)) {
                    return true;
                }
            } else {
                // 精确匹配或前缀匹配
                if (path.equals(whitePath) || path.startsWith(whitePath + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

}
