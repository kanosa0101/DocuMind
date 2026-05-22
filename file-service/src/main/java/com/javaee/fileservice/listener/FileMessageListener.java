package com.javaee.fileservice.listener;

import com.javaee.fileservice.config.RabbitMQConfig;
import com.javaee.fileservice.chain.FileProcessChain;
import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件服务消息监听器
 * v3.0: 文件上传消息由file-service处理，触发AI处理链
 */
@Component
public class FileMessageListener {

    private static final Logger log = LoggerFactory.getLogger(FileMessageListener.class);

    private final FileInfoMapper fileInfoMapper;
    private final FileProcessChain fileProcessChain;

    public FileMessageListener(FileInfoMapper fileInfoMapper, FileProcessChain fileProcessChain) {
        this.fileInfoMapper = fileInfoMapper;
        this.fileProcessChain = fileProcessChain;
    }

    /**
     * 处理文件上传消息
     * v3.0: 触发AI处理流程
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_UPLOAD_QUEUE)
    public void handleFileUploadMessage(Map<String, Object> message) {
        log.info("=== 收到文件上传消息 ===");
        log.info("消息内容: {}", message);

        try {
            String fileId = (String) message.get("fileId");
            String fileName = (String) message.get("fileName");
            Long fileSize = message.get("fileSize") instanceof Integer
                ? ((Integer) message.get("fileSize")).longValue()
                : (Long) message.get("fileSize");
            Long userId = message.get("userId") instanceof Integer
                ? ((Integer) message.get("userId")).longValue()
                : (Long) message.get("userId");
            String action = (String) message.get("action");

            log.info("文件ID: {}, 文件名: {}, 用户ID: {}, 动作: {}", fileId, fileName, userId, action);

            // v3.0: FileInfo已由Controller的upload/uploadNewVersion创建，这里只需要触发AI处理链
            FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileId);
            if (fileInfo == null) {
                log.warn("FileInfo不存在，可能已被删除: fileId={}", fileId);
                return;
            }

            // 触发AI处理链（NEW和UPDATE_VERSION都需要重新处理AI）
            if ("NEW".equals(action) || "UPDATE_VERSION".equals(action)) {
                log.info("开始执行AI处理链: fileId={}, action={}", fileId, action);

                ProcessContext context = new ProcessContext();
                context.setFileUuid(fileId);
                context.setUserId(userId);
                context.setFileName(fileName);
                context.setStoragePath(fileInfo.getStoragePath());

                // v3.0: UPDATE_VERSION时标记版本更新
                if ("UPDATE_VERSION".equals(action)) {
                    context.setVersionUpdate(true);
                    context.setChangeSummary((String) message.get("changeSummary"));
                }

                fileProcessChain.execute(context);
                log.info("AI处理链执行完成: fileId={}, status={}", fileId, context.getProcessStatus());
            }

            log.info("文件上传消息处理完成");
        } catch (Exception e) {
            log.error("处理文件上传消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理文件下载消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_DOWNLOAD_QUEUE)
    public void handleFileDownloadMessage(Map<String, Object> message) {
        log.info("=== 收到文件下载消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());

        String fileId = (String) message.get("fileId");
        String userId = (String) message.get("userId");

        log.info("文件ID: {}, 用户ID: {}", fileId, userId);

        log.info("文件下载消息处理完成");
    }

    /**
     * 处理文件处理消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_PROCESS_QUEUE)
    public void handleFileProcessMessage(Map<String, Object> message) {
        log.info("=== 收到文件处理消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());

        String fileId = (String) message.get("fileId");
        String processType = (String) message.get("processType");

        log.info("文件ID: {}, 处理类型: {}", fileId, processType);

        log.info("文件处理消息处理完成");
    }

    /**
     * 处理文件删除消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_DELETE_QUEUE)
    public void handleFileDeleteMessage(Map<String, Object> message) {
        log.info("=== 收到文件删除消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());

        String fileId = (String) message.get("fileId");
        String userId = (String) message.get("userId");

        log.info("文件ID: {}, 用户ID: {}", fileId, userId);

        log.info("文件删除消息处理完成");
    }
}