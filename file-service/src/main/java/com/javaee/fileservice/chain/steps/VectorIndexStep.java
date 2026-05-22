package com.javaee.fileservice.chain.steps;

import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.chain.ProcessResult;
import com.javaee.fileservice.chain.ProcessStep;
import com.javaee.fileservice.chain.RecoveryAction;
import com.javaee.fileservice.client.AIServiceClient;
import com.javaee.fileservice.config.FeignConfig;
import com.javaee.fileservice.state.ProcessState;
import com.javaee.common.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 向量索引步骤 (v3.0)
 * 将文档索引到知识库中
 */
@Component
public class VectorIndexStep implements ProcessStep {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexStep.class);

    @Autowired
    private AIServiceClient aiServiceClient;

    @Override
    public String getStepName() {
        return "VECTOR_INDEX";
    }

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public ProcessResult execute(ProcessContext context) {
        log.info("开始向量索引: fileUuid={}, version={}, userId={}",
                 context.getFileUuid(), context.getVersion(), context.getUserId());

        String content = context.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("内容为空，跳过索引: fileUuid={}", context.getFileUuid());
            context.setIndexed(false);
            return ProcessResult.success("内容为空，跳过索引");
        }

        try {
            FeignConfig.setUserContext(context.getUserId(), "system");

            // v3.0：使用版本后缀ID格式 (fileUuid_v版本号)
            Integer version = context.getVersion() != null ? context.getVersion() : 1;
            String vectorId = context.getFileUuid() + "_v" + version;

            // 传递userId作为X-User-Id请求头，用于知识库用户隔离
            // 使用版本后缀ID作为documentId
            Result<Void> result = aiServiceClient.indexDocument(
                    vectorId,
                    context.getFileName(),
                    content,
                    String.valueOf(context.getUserId())
            );

            if (result != null && result.getCode() == 200) {
                context.setIndexed(true);
                context.setVectorId(vectorId);  // v3.0：记录向量ID
                context.setProcessStatus(ProcessState.INDEXING);

                log.info("向量索引完成: fileUuid={}, vectorId={}", context.getFileUuid(), vectorId);
                return ProcessResult.success("索引成功");
            } else {
                String errorMsg = result != null ? result.getMessage() : "索引服务调用失败";
                log.warn("向量索引失败: fileUuid={}, error={}", context.getFileUuid(), errorMsg);
                return ProcessResult.fail(errorMsg);
            }
        } catch (Exception e) {
            log.error("向量索引异常: fileUuid={}", context.getFileUuid(), e);
            return ProcessResult.fail(e);
        } finally {
            FeignConfig.clearUserContext();
        }
    }

    @Override
    public boolean shouldSkip(ProcessContext context) {
        return context.isIndexed();
    }

    @Override
    public RecoveryAction onError(ProcessContext context, Exception error) {
        return RecoveryAction.RETRY;
    }
}