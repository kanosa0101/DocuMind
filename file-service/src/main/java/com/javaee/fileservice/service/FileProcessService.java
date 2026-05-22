package com.javaee.fileservice.service;

import com.javaee.fileservice.chain.FileProcessChain;
import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.util.DocumentParserUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文件处理服务 (v3.0)
 * 触发处理链对上传的文件进行AI处理
 * 支持异步执行，不阻塞上传响应
 */
@Service
public class FileProcessService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessService.class);

    @Autowired
    private FileProcessChain fileProcessChain;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private com.javaee.fileservice.config.MinioConfig minioConfig;

    /**
     * 处理文件（异步执行）
     * v3.0：使用@Async异步执行处理链，不阻塞上传响应
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     */
    @Async("fileProcessExecutor")
    public void processFile(String fileUuid, Long userId) {
        log.info("开始异步处理文件: fileUuid={}, userId={}, thread={}",
                 fileUuid, userId, Thread.currentThread().getName());

        FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileUuid);
        if (fileInfo == null) {
            log.error("文件不存在: fileUuid={}", fileUuid);
            return;
        }

        ProcessContext context = new ProcessContext();
        context.setFileUuid(fileUuid);
        context.setFileName(fileInfo.getOriginalName());
        context.setUserId(userId);
        context.setAction("NEW");
        context.setVersion(fileInfo.getVersion() != null ? fileInfo.getVersion() : 1);
        context.setStartTime(System.currentTimeMillis());

        // 从MinIO下载文件并解析内容
        try {
            String objectName = extractObjectName(fileInfo.getStoragePath());
            byte[] fileContent = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build()
            ).readAllBytes();

            String content = DocumentParserUtil.parseDocument(fileContent, fileInfo.getOriginalName());
            context.setContent(content);
            log.info("文件内容解析完成: fileUuid={}, contentLength={}", fileUuid, content.length());
        } catch (Exception e) {
            log.error("下载或解析文件失败: fileUuid={}", fileUuid, e);
            context.setContent("");
            context.addError("解析失败: " + e.getMessage());
        }

        // 执行处理链（异步）
        fileProcessChain.execute(context);

        context.setEndTime(System.currentTimeMillis());
        log.info("文件处理完成: fileUuid={}, duration={}ms", fileUuid, context.getDuration());
    }

    /**
     * 从storagePath提取objectName
     */
    private String extractObjectName(String storagePath) {
        if (storagePath == null || !storagePath.contains("/")) {
            return storagePath;
        }
        return storagePath.substring(storagePath.indexOf("/") + 1);
    }
}