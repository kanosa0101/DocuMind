package com.javaee.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * UTF-8编码过滤器
 * 确保请求体正确使用UTF-8编码转发到下游服务
 */
@Component
public class EncodingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 只处理JSON请求
        MediaType contentType = request.getHeaders().getContentType();
        if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
            // 确保Content-Type包含charset=UTF-8
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(request.getHeaders());
            if (!headers.containsKey("Content-Type") ||
                !headers.get("Content-Type").get(0).contains("charset")) {
                headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            }

            // 创建新的请求，确保编码正确
            ServerHttpRequest newRequest = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }
            };

            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在RateLimitFilter之后，AuthGlobalFilter之前执行
        return -150;
    }
}