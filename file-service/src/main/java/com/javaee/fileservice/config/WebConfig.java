package com.javaee.fileservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 * 确保Spring正确处理UTF-8编码的请求和响应
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Spring Boot 3.x 默认使用UTF-8编码
    // 此配置类用于确保所有请求正确处理中文内容

}