package com.javaee.fileservice.websocket;

import com.javaee.fileservice.chain.ProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进度WebSocket处理器 (v3.0)
 * 向前端推送文件处理进度和完成通知
 */
@Component
public class ProgressWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProgressWebSocketHandler.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册用户会话
     */
    public void registerSession(Long userId, String sessionId) {
        userSessions.put(userId, sessionId);
        log.info("用户WebSocket会话注册: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 移除用户会话
     */
    public void removeSession(Long userId) {
        userSessions.remove(userId);
        log.info("用户WebSocket会话移除: userId={}", userId);
    }

    /**
     * 推送处理进度
     */
    public void pushProgress(Long userId, ProcessContext context) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("PROGRESS");
        msg.setFileUuid(context.getFileUuid());
        msg.setStep(context.getCurrentStep());
        msg.setProgress(context.getProgress());
        msg.setStatus(context.getProcessStatus() != null ? context.getProcessStatus().getDescription() : "处理中");

        sendMessage(userId, msg);
        log.debug("推送进度: userId={}, step={}, progress={}", userId, context.getCurrentStep(), context.getProgress());
    }

    /**
     * 推送完成通知
     */
    public void pushComplete(Long userId, String fileUuid, boolean success) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("COMPLETE");
        msg.setFileUuid(fileUuid);
        msg.setSuccess(success);
        msg.setMessage(success ? "智能整理完成" : "处理失败，点击重试");

        sendMessage(userId, msg);
        log.info("推送完成通知: userId={}, fileUuid={}, success={}", userId, fileUuid, success);
    }

    /**
     * 推送错误通知
     */
    public void pushError(Long userId, String fileUuid, String error) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("ERROR");
        msg.setFileUuid(fileUuid);
        msg.setMessage(error);

        sendMessage(userId, msg);
        log.warn("推送错误通知: userId={}, fileUuid={}, error={}", userId, fileUuid, error);
    }

    /**
     * 发送消息到用户
     */
    private void sendMessage(Long userId, WebSocketMessage msg) {
        try {
            String destination;
            if ("PROGRESS".equals(msg.getType())) {
                destination = "/topic/progress/" + userId;
            } else {
                destination = "/topic/complete/" + userId;
            }

            messagingTemplate.convertAndSend(destination, msg);
            log.debug("WebSocket消息已发送: userId={}, type={}, destination={}", userId, msg.getType(), destination);
        } catch (Exception e) {
            log.error("WebSocket消息发送失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * WebSocket消息结构
     */
    public static class WebSocketMessage {
        private String type;
        private String fileUuid;
        private String step;
        private Integer progress;
        private String status;
        private Boolean success;
        private String message;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getFileUuid() { return fileUuid; }
        public void setFileUuid(String fileUuid) { this.fileUuid = fileUuid; }

        public String getStep() { return step; }
        public void setStep(String step) { this.step = step; }

        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}