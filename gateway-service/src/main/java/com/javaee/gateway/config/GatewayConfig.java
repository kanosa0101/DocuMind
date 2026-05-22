package com.javaee.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置 (v3.0)
 * 支持服务发现模式（lb://）和直连模式（http://）
 * v3.0新增：file-service v3路由、WebSocket路由迁移
 */
@Configuration
public class GatewayConfig {

    @Value("${gateway.route.user-uri:lb://user}")
    private String userServiceUri;

    @Value("${gateway.route.file-uri:lb://file}")
    private String fileServiceUri;

    @Value("${gateway.route.ai-uri:lb://ai-service}")
    private String aiServiceUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务路由
                .route("user", r -> r.path("/api/users/**")
                        .uri(userServiceUri))
                // 文件服务路由 (v2.0保留)
                .route("file", r -> r.path("/api/files/**")
                        .uri(fileServiceUri))
                // 文件服务v3.0路由 (新增)
                .route("file-v3", r -> r.path("/api/v3/files/**")
                        .uri(fileServiceUri))
                // AI服务路由（包含RAG接口：/api/ai/rag/**）
                .route("ai", r -> r.path("/api/ai/**")
                        .uri(aiServiceUri))
                // WebSocket路由 - v3.0迁移到file-service
                .route("websocket-file", r -> r.path("/ws/progress/**")
                        .uri(fileServiceUri))
                .build();
    }

}
