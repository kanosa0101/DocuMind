package com.javaee.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 注意：网关是响应式架构，不使用Servlet Filter
 * scanBasePackages 只扫描 common.util 包（RabbitMQUtil），不扫描security包
 */
@SpringBootApplication(scanBasePackages = {"com.javaee.gateway", "com.javaee.common.util"})
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
