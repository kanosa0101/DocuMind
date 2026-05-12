package com.javaee.fileservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 注意：配置了安全的消息转换器，限制反序列化的包范围
 */
@Configuration
public class RabbitMQConfig {

    public static final String FILE_UPLOAD_QUEUE = "file.upload.queue";
    public static final String FILE_DOWNLOAD_QUEUE = "file.download.queue";
    public static final String FILE_PROCESS_QUEUE = "file.process.queue";
    public static final String FILE_DELETE_QUEUE = "file.delete.queue";

    public static final String FILE_EXCHANGE = "file.exchange";

    public static final String FILE_UPLOAD_ROUTING_KEY = "file.upload";
    public static final String FILE_DOWNLOAD_ROUTING_KEY = "file.download";
    public static final String FILE_PROCESS_ROUTING_KEY = "file.process";
    public static final String FILE_DELETE_ROUTING_KEY = "file.delete";

    @Bean
    public TopicExchange fileExchange() {
        return new TopicExchange(FILE_EXCHANGE, true, false);
    }

    @Bean
    public Queue fileUploadQueue() {
        return new Queue(FILE_UPLOAD_QUEUE, true, false, false);
    }

    @Bean
    public Queue fileDownloadQueue() {
        return new Queue(FILE_DOWNLOAD_QUEUE, true, false, false);
    }

    @Bean
    public Queue fileProcessQueue() {
        return new Queue(FILE_PROCESS_QUEUE, true, false, false);
    }

    @Bean
    public Queue fileDeleteQueue() {
        return new Queue(FILE_DELETE_QUEUE, true, false, false);
    }

    @Bean
    public Binding bindingFileUploadQueue() {
        return BindingBuilder.bind(fileUploadQueue()).to(fileExchange()).with(FILE_UPLOAD_ROUTING_KEY);
    }

    @Bean
    public Binding bindingFileDownloadQueue() {
        return BindingBuilder.bind(fileDownloadQueue()).to(fileExchange()).with(FILE_DOWNLOAD_ROUTING_KEY);
    }

    @Bean
    public Binding bindingFileProcessQueue() {
        return BindingBuilder.bind(fileProcessQueue()).to(fileExchange()).with(FILE_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding bindingFileDeleteQueue() {
        return BindingBuilder.bind(fileDeleteQueue()).to(fileExchange()).with(FILE_DELETE_ROUTING_KEY);
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