package com.javaee.user.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 用户服务相关队列
    public static final String USER_REGISTER_QUEUE = "user.register.queue";
    public static final String USER_PASSWORD_RESET_QUEUE = "user.password.reset.queue";
    public static final String USER_OPERATE_LOG_QUEUE = "user.operate.log.queue";
    
    // 用户服务相关交换机
    public static final String USER_EXCHANGE = "user.exchange";
    
    // 用户服务相关路由键
    public static final String USER_REGISTER_ROUTING_KEY = "user.register";
    public static final String USER_PASSWORD_RESET_ROUTING_KEY = "user.password.reset";
    public static final String USER_OPERATE_LOG_ROUTING_KEY = "user.operate.log";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    /**
     * 声明用户注册队列
     */
    @Bean
    public Queue userRegisterQueue() {
        return new Queue(USER_REGISTER_QUEUE, true, false, false);
    }

    /**
     * 声明密码重置队列
     */
    @Bean
    public Queue userPasswordResetQueue() {
        return new Queue(USER_PASSWORD_RESET_QUEUE, true, false, false);
    }

    /**
     * 声明操作日志队列
     */
    @Bean
    public Queue userOperateLogQueue() {
        return new Queue(USER_OPERATE_LOG_QUEUE, true, false, false);
    }

    /**
     * 绑定用户注册队列到交换机
     */
    @Bean
    public Binding bindingUserRegisterQueue() {
        return BindingBuilder.bind(userRegisterQueue()).to(userExchange()).with(USER_REGISTER_ROUTING_KEY);
    }

    /**
     * 绑定密码重置队列到交换机
     */
    @Bean
    public Binding bindingUserPasswordResetQueue() {
        return BindingBuilder.bind(userPasswordResetQueue()).to(userExchange()).with(USER_PASSWORD_RESET_ROUTING_KEY);
    }

    /**
     * 绑定操作日志队列到交换机
     */
    @Bean
    public Binding bindingUserOperateLogQueue() {
        return BindingBuilder.bind(userOperateLogQueue()).to(userExchange()).with(USER_OPERATE_LOG_ROUTING_KEY);
    }
}
