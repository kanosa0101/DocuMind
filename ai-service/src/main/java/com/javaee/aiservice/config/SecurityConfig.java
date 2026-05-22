package com.javaee.aiservice.config;

import com.javaee.common.config.security.BaseSecurityConfig;
import com.javaee.common.config.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * AI服务安全配置
 * 注意：AI服务接口需要认证，但内部服务调用接口放行
 * 继承BaseSecurityConfig，正确配置JWT认证过滤器
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends BaseSecurityConfig {

    /**
     * 构造函数注入JWT认证过滤器
     * @param jwtAuthenticationFilter JWT认证过滤器
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        super(jwtAuthenticationFilter);
    }

    /**
     * 配置SecurityFilterChain
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 应用基础安全配置
        applyBaseSecurityConfig(http);

        http
            // 授权配置
            .authorizeHttpRequests(authorize -> authorize
                // 确保Swagger相关路径的匹配规则在最前面
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // 允许健康检查等端点访问
                .requestMatchers("/actuator/**").permitAll()
                // 允许错误处理端点访问
                .requestMatchers("/error").permitAll()
                // 允许流式SSE endpoint（用于前端实时显示）
                .requestMatchers("/api/ai/rag/query/stream", "/api/ai/agent/chat/stream").permitAll()
                // v3.0: 内部服务调用接口放行（file-service通过FeignClient调用）
                // 注意路径要与RagController一致：/api/ai/rag/xxx
                .requestMatchers("/api/ai/summarize", "/api/ai/keywords", "/api/ai/classify").permitAll()
                .requestMatchers("/api/ai/rag/index", "/api/ai/rag/document/*", "/api/ai/rag/documents", "/api/ai/rag/search").permitAll()
                // AI接口需要认证
                .requestMatchers("/api/ai/**").authenticated()
                // 其他接口需要认证
                .anyRequest().authenticated()
            );

        return http.build();
    }
}