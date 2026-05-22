package com.javaee.fileservice;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文件服务应用启动类 (v3.0)
 */
@SpringBootApplication(scanBasePackages = {"com.javaee.fileservice", "com.javaee.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.javaee.fileservice.client")
@MapperScan(basePackages = "com.javaee.fileservice.mapper")
@EnableScheduling
public class FileServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(FileServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FileServiceApplication.class);
        Environment environment = application.run(args).getEnvironment();
        log.info("Application started: {}, port: {}",
            environment.getProperty("spring.application.name"),
            environment.getProperty("server.port"));
    }

}
