package com.javaee.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 安全配置
 * 注意：网关通过AuthGlobalFilter进行JWT认证，此处配置基本安全策略
 * Spring Security放行所有请求，由AuthGlobalFilter统一处理认证逻辑
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            // 禁用CSRF保护
            .csrf(csrf -> csrf.disable())
            // 禁用表单登录
            .formLogin(formLogin -> formLogin.disable())
            // 禁用HTTP Basic认证
            .httpBasic(httpBasic -> httpBasic.disable())
            // 放行所有请求，由AuthGlobalFilter统一处理JWT认证
            // 这样避免Spring Security在AuthGlobalFilter之前拦截请求导致认证失败
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            );
        return http.build();
    }

}