package com.javaee.fileservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign配置类 (v3.0)
 * 用于服务间调用传递用户上下文
 */
@Configuration
public class FeignConfig {

    private static final ThreadLocal<Long> userIdContext = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameContext = new ThreadLocal<>();

    /**
     * 设置用户上下文
     */
    public static void setUserContext(Long userId, String username) {
        userIdContext.set(userId);
        usernameContext.set(username);
    }

    /**
     * 清除用户上下文
     */
    public static void clearUserContext() {
        userIdContext.remove();
        usernameContext.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return userIdContext.get();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        return usernameContext.get();
    }

    /**
     * Feign请求拦截器 - 传递用户上下文到请求头
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Long userId = getCurrentUserId();
            String username = getCurrentUsername();
            if (userId != null) {
                requestTemplate.header("X-User-Id", String.valueOf(userId));
            }
            if (username != null) {
                requestTemplate.header("X-Username", username);
            }
        };
    }
}