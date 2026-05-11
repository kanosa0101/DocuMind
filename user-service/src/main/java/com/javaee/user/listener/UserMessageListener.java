package com.javaee.user.listener;

import com.javaee.user.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户服务消息监听器
 */
@Slf4j
@Component
public class UserMessageListener {

    /**
     * 处理用户注册消息
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTER_QUEUE)
    public void handleUserRegisterMessage(Map<String, Object> message) {
        log.info("=== 收到用户注册消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        Long userId = message.get("userId") instanceof Integer ? 
            ((Integer) message.get("userId")).longValue() : (Long) message.get("userId");
        String username = (String) message.get("username");
        String email = (String) message.get("email");
        
        log.info("用户ID: {}, 用户名: {}, 邮箱: {}", userId, username, email);
        
        log.info("用户注册消息处理完成 - 可以在这里发送欢迎邮件");
    }

    /**
     * 处理密码重置消息
     */
    @RabbitListener(queues = RabbitMQConfig.USER_PASSWORD_RESET_QUEUE)
    public void handlePasswordResetMessage(Map<String, Object> message) {
        log.info("=== 收到密码重置消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        Long userId = message.get("userId") instanceof Integer ? 
            ((Integer) message.get("userId")).longValue() : (Long) message.get("userId");
        String username = (String) message.get("username");
        
        log.info("用户ID: {}, 用户名: {}", userId, username);
        
        log.info("密码重置消息处理完成 - 可以在这里发送重置链接邮件");
    }

    /**
     * 处理用户操作日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.USER_OPERATE_LOG_QUEUE)
    public void handleOperateLogMessage(Map<String, Object> message) {
        log.info("=== 收到用户操作日志消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        Long userId = message.get("userId") instanceof Integer ? 
            ((Integer) message.get("userId")).longValue() : (Long) message.get("userId");
        String operation = (String) message.get("operation");
        String description = (String) message.get("description");
        
        log.info("用户ID: {}, 操作: {}, 描述: {}", userId, operation, description);
        
        log.info("用户操作日志消息处理完成 - 可以在这里记录到数据库");
    }
}
