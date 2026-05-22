package com.javaee.fileservice;

import com.javaee.fileservice.util.RedisUtil;
import com.javaee.fileservice.client.AIServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 测试配置类
 * Mock Feign客户端和Redis依赖，确保测试能正常启动
 */
@TestConfiguration
public class TestConfig {

    @MockBean
    private AIServiceClient aiServiceClient;

    @MockBean
    private RedisUtil redisUtil;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
}