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

import java.util.HashMap;
import java.util.Map;

/**
 * AI摘要生成步骤 (v3.0)
 * 调用AI服务生成文档摘要
 */
@Component
public class AISummarizeStep implements ProcessStep {

    private static final Logger log = LoggerFactory.getLogger(AISummarizeStep.class);
    private static final int DEFAULT_MAX_LENGTH = 200;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Override
    public String getStepName() {
        return "AI_SUMMARIZE";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public ProcessResult execute(ProcessContext context) {
        log.info("开始生成AI摘要: fileUuid={}, userId={}", context.getFileUuid(), context.getUserId());

        String content = context.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("内容为空，无法生成摘要: fileUuid={}", context.getFileUuid());
            context.setSummary("");
            return ProcessResult.success("内容为空，摘要为空");
        }

        try {
            FeignConfig.setUserContext(context.getUserId(), "system");

            Map<String, Object> request = new HashMap<>();
            request.put("content", content);
            request.put("maxLength", DEFAULT_MAX_LENGTH);

            Result<Map<String, Object>> result = aiServiceClient.summarize(request);

            if (result != null && result.getCode() == 200 && result.getData() != null) {
                String summary = (String) result.getData().get("summary");
                context.setSummary(summary);
                context.setProcessStatus(ProcessState.AI_PROCESSING);

                log.info("AI摘要生成完成: fileUuid={}, summaryLength={}",
                        context.getFileUuid(), summary != null ? summary.length() : 0);
                return ProcessResult.success("摘要生成成功");
            } else {
                String errorMsg = result != null ? result.getMessage() : "AI服务调用失败";
                log.warn("AI摘要生成失败: fileUuid={}, error={}", context.getFileUuid(), errorMsg);
                return ProcessResult.fail(errorMsg);
            }
        } catch (Exception e) {
            log.error("AI摘要生成异常: fileUuid={}", context.getFileUuid(), e);
            return ProcessResult.fail(e);
        } finally {
            FeignConfig.clearUserContext();
        }
    }

    @Override
    public boolean shouldSkip(ProcessContext context) {
        return context.getSummary() != null;
    }

    @Override
    public RecoveryAction onError(ProcessContext context, Exception error) {
        return RecoveryAction.RETRY;
    }
}