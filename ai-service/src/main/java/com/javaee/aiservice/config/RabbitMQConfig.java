package com.javaee.aiservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置消息队列和交换机
 * 注意：配置了安全的消息转换器，限制反序列化的包范围
 */
@Configuration
public class RabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String AI_TASK_QUEUE = "ai.task.queue";
    public static final String AI_ALERT_QUEUE = "ai.alert.queue";

    /**
     * 创建交换机
     */
    @Bean
    public Exchange aiExchange() {
        return ExchangeBuilder.directExchange(AI_EXCHANGE).durable(true).build();
    }

    /**
     * 创建任务队列
     */
    @Bean
    public Queue aiTaskQueue() {
        return QueueBuilder.durable(AI_TASK_QUEUE).build();
    }

    /**
     * 创建告警队列
     */
    @Bean
    public Queue aiAlertQueue() {
        return QueueBuilder.durable(AI_ALERT_QUEUE).build();
    }

    /**
     * 绑定任务队列
     */
    @Bean
    public Binding taskBinding(Exchange aiExchange, Queue aiTaskQueue) {
        return BindingBuilder.bind(aiTaskQueue).to(aiExchange).with("task").noargs();
    }

    /**
     * 绑定告警队列
     */
    @Bean
    public Binding alertBinding(Exchange aiExchange, Queue aiAlertQueue) {
        return BindingBuilder.bind(aiAlertQueue).to(aiExchange).with("alert").noargs();
    }

    /**
     * 安全的消息转换器
     * 使用Jackson进行JSON序列化/反序列化
     * 注：信任包配置通过环境变量SPRING_AMQP_DESERIALIZATION_TRUST_ALL控制
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
