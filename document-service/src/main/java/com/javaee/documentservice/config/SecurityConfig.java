package com.javaee.documentservice.config;

import com.javaee.common.config.security.BaseSecurityConfig;
import com.javaee.common.config.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置类
 * 注意：收紧权限设置，需要认证才能访问文档接口
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
                // 文档接口需要认证（移除了过于宽松的 permitAll）
                .requestMatchers("/api/documents/**").authenticated()
                // 其他接口需要认证
                .anyRequest().authenticated()
            );

        return http.build();
    }
}