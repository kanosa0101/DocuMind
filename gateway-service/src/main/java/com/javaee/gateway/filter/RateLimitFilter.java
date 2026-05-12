package com.javaee.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis限流过滤器
 * 使用令牌桶算法实现请求限流，防止系统过载
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Value("${custom.rate-limit.enable:true}")
    private boolean enableRateLimit;

    @Value("${custom.rate-limit.redis-key-prefix:gateway:rate-limit}")
    private String redisKeyPrefix;

    @Value("${custom.rate-limit.default-rate:100}")
    private long defaultRate;

    @Value("${custom.rate-limit.default-burst:200}")
    private long defaultBurst;

    // 限流时间窗口（秒）
    private static final long WINDOW_SIZE = 1;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 如果限流未启用，直接放行
        if (!enableRateLimit) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 获取客户端IP作为限流key
        String clientIp = getClientIp(request);
        String key = redisKeyPrefix + ":" + clientIp + ":" + path;

        return tryAcquire(key)
                .flatMap(acquired -> {
                    if (acquired) {
                        log.debug("请求通过限流: {} from {}", path, clientIp);
                        return chain.filter(exchange);
                    } else {
                        log.warn("请求被限流: {} from {}", path, clientIp);
                        return tooManyRequests(exchange);
                    }
                })
                .onErrorResume(e -> {
                    log.error("限流检查异常，放行请求: {}", e.getMessage());
                    // Redis异常时放行请求，避免影响服务可用性
                    return chain.filter(exchange);
                });
    }

    /**
     * 尝试获取令牌（使用滑动窗口算法）
     * @param key Redis key
     * @return 是否成功获取
     */
    private Mono<Boolean> tryAcquire(String key) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - TimeUnit.SECONDS.toMillis(WINDOW_SIZE);

        return redisTemplate.opsForZSet()
                .range(key, Range.closed(windowStart, currentTime))
                .count()
                .flatMap(currentCount -> {
                    if (currentCount < defaultRate) {
                        // 添加当前请求记录
                        return redisTemplate.opsForZSet()
                                .add(key, String.valueOf(currentTime), currentTime)
                                .then(redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SIZE + 1)))
                                .thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    /**
     * 获取客户端IP
     * @param request 请求
     * @return IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        // 对于多代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 返回限流响应
     * @param exchange 交换
     * @return 响应
     */
    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(defaultRate));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        // 在AuthGlobalFilter之前执行，优先级更高
        return -200;
    }
}