package com.javaee.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Reactive Redis配置
 * 为网关限流等响应式功能提供ReactiveStringRedisTemplate
 */
@Configuration
public class ReactiveRedisConfig {

    /**
     * 配置ReactiveStringRedisTemplate
     * 使用String序列化器，确保key和value都是字符串形式
     * @param connectionFactory ReactiveRedis连接工厂
     * @return ReactiveStringRedisTemplate实例
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext(stringSerializer)
                .key(stringSerializer)
                .value(stringSerializer)
                .hashKey(stringSerializer)
                .hashValue(stringSerializer)
                .build();

        return new ReactiveStringRedisTemplate(connectionFactory, serializationContext);
    }
}