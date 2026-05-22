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
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * AI关键词提取步骤 (v3.0)
 * 调用AI服务提取文档关键词
 */
@Component
public class AIKeywordsStep implements ProcessStep {

    private static final Logger log = LoggerFactory.getLogger(AIKeywordsStep.class);
    private static final int DEFAULT_KEYWORD_COUNT = 5;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Override
    public String getStepName() {
        return "AI_KEYWORDS";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public ProcessResult execute(ProcessContext context) {
        log.info("开始提取AI关键词: fileUuid={}, userId={}", context.getFileUuid(), context.getUserId());

        String content = context.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("内容为空，无法提取关键词: fileUuid={}", context.getFileUuid());
            context.setKeywords(List.of());
            return ProcessResult.success("内容为空，关键词为空");
        }

        try {
            FeignConfig.setUserContext(context.getUserId(), "system");

            Map<String, Object> request = new HashMap<>();
            request.put("content", content);
            request.put("count", DEFAULT_KEYWORD_COUNT);

            Result<Map<String, Object>> result = aiServiceClient.extractKeywords(request);

            if (result != null && result.getCode() == 200 && result.getData() != null) {
                // v3.0 fix: 处理两种返回格式
                // 格式1: keywords是List<KeywordVO>对象（LinkedHashMap序列化）
                // 格式2: keywords是List<String>
                Object keywordsObj = result.getData().get("keywords");
                List<String> keywords = new ArrayList<>();

                if (keywordsObj instanceof List) {
                    List<?> keywordList = (List<?>) keywordsObj;
                    keywords = keywordList.stream()
                        .map(item -> {
                            if (item instanceof String) {
                                return (String) item;
                            } else if (item instanceof Map) {
                                // KeywordVO序列化为Map，提取word字段
                                Map<?, ?> kwMap = (Map<?, ?>) item;
                                Object word = kwMap.get("word");
                                return word != null ? word.toString() : "";
                            }
                            return item.toString();
                        })
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                }

                context.setKeywords(keywords);
                context.setProcessStatus(ProcessState.AI_PROCESSING);

                log.info("AI关键词提取完成: fileUuid={}, keywordsCount={}",
                        context.getFileUuid(), context.getKeywords().size());
                return ProcessResult.success("关键词提取成功");
            } else {
                String errorMsg = result != null ? result.getMessage() : "AI服务调用失败";
                log.warn("AI关键词提取失败: fileUuid={}, error={}", context.getFileUuid(), errorMsg);
                return ProcessResult.fail(errorMsg);
            }
        } catch (Exception e) {
            log.error("AI关键词提取异常: fileUuid={}", context.getFileUuid(), e);
            return ProcessResult.fail(e);
        } finally {
            FeignConfig.clearUserContext();
        }
    }

    @Override
    public boolean shouldSkip(ProcessContext context) {
        return context.getKeywords() != null && !context.getKeywords().isEmpty();
    }

    @Override
    public RecoveryAction onError(ProcessContext context, Exception error) {
        return RecoveryAction.RETRY;
    }
}