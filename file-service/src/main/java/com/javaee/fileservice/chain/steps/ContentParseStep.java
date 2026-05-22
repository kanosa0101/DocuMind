package com.javaee.fileservice.chain.steps;

import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.chain.ProcessResult;
import com.javaee.fileservice.chain.ProcessStep;
import com.javaee.fileservice.chain.RecoveryAction;
import com.javaee.fileservice.config.MinioConfig;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.state.ProcessState;
import com.javaee.fileservice.util.DocumentParserUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.GetObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * 内容解析步骤 (v3.0)
 * 解析上传文件的内容
 */
@Component
public class ContentParseStep implements ProcessStep {

    private static final Logger log = LoggerFactory.getLogger(ContentParseStep.class);

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Override
    public String getStepName() {
        return "CONTENT_PARSE";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public ProcessResult execute(ProcessContext context) {
        log.info("开始解析文件内容: fileUuid={}, userId={}", context.getFileUuid(), context.getUserId());

        try {
            FileInfo fileInfo = fileInfoMapper.selectByFileUuid(context.getFileUuid());
            if (fileInfo == null) {
                return ProcessResult.fail("文件不存在: " + context.getFileUuid());
            }

            // 从MinIO下载文件内容
            if (context.getContent() == null || context.getContent().isEmpty()) {
                String storagePath = context.getStoragePath();
                if (storagePath == null || storagePath.isEmpty()) {
                    storagePath = fileInfo.getStoragePath();
                }

                if (storagePath != null && !storagePath.isEmpty() && fileInfo.getFileName() != null) {
                    try {
                        // v3.0 fix: 从fileName获取扩展名，构建正确的MinIO object name
                        String fileUuid = context.getFileUuid();
                        String fileName = fileInfo.getFileName();
                        String objectName = fileUuid;
                        int lastDot = fileName.lastIndexOf('.');
                        if (lastDot > 0) {
                            objectName = fileUuid + fileName.substring(lastDot);
                        }

                        log.info("从MinIO下载文件: bucket={}, object={}", minioConfig.getBucket(), objectName);

                        GetObjectResponse response = minioClient.getObject(
                            GetObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(objectName)
                                .build()
                        );

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        response.transferTo(outputStream);
                        byte[] fileContent = outputStream.toByteArray();

                        // 使用DocumentParserUtil解析文件内容
                        String content = DocumentParserUtil.parseDocument(fileContent, fileInfo.getFileName());
                        context.setContent(content);

                        log.info("文件内容下载并解析完成: fileUuid={}, contentLength={}",
                            context.getFileUuid(), content.length());
                    } catch (Exception e) {
                        log.error("从MinIO下载文件失败: fileUuid={}, storagePath={}", context.getFileUuid(), storagePath, e);
                        context.setContent("");
                        return ProcessResult.fail("文件下载失败: " + e.getMessage());
                    }
                } else {
                    log.warn("文件存储路径或文件名为空: fileUuid={}", context.getFileUuid());
                    context.setContent("");
                }
            }

            context.setProcessStatus(ProcessState.PARSE);

            log.info("文件内容解析完成: fileUuid={}, contentLength={}", context.getFileUuid(),
                    context.getContent() != null ? context.getContent().length() : 0);
            return ProcessResult.success("内容解析成功");
        } catch (Exception e) {
            log.error("文件内容解析失败: fileUuid={}", context.getFileUuid(), e);
            return ProcessResult.fail(e);
        }
    }

    @Override
    public boolean shouldSkip(ProcessContext context) {
        return context.getContent() != null && !context.getContent().isEmpty();
    }

    @Override
    public RecoveryAction onError(ProcessContext context, Exception error) {
        log.warn("内容解析失败，将跳过此步骤");
        context.setContent("");
        return RecoveryAction.SKIP;
    }
}