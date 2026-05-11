package com.javaee.aiservice.agent;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 自定义Chat服务
 * 适配阿里云百炼dashscope的DeepSeek模型
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.chat.model:deepseek-v3.2}")
    private String model;

    /**
     * 调用阿里云百炼Chat API
     * @param prompt 用户提示词
     * @return 响应内容
     */
    public String callChatApi(String prompt) {
        log.info("调用阿里云百炼Chat API: model={}, prompt length={}", model, prompt.length());

        try {
            Generation gen = new Generation();
            
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .enableThinking(true)
                    .incrementalOutput(true)
                    .resultFormat("message")
                    .messages(Arrays.asList(userMsg))
                    .build();

            GenerationResult result = gen.call(param);
            
            if (result != null && result.getOutput() != null 
                    && result.getOutput().getChoices() != null 
                    && !result.getOutput().getChoices().isEmpty()) {
                
                String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                String reasoning = result.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                
                System.out.println("========== Chat响应内容开始 ==========");
                if (reasoning != null && !reasoning.isEmpty()) {
                    System.out.println("思考过程: " + reasoning);
                }
                System.out.println("回复内容: " + content);
                System.out.println("========== Chat响应内容结束 ==========");
                
                return content != null ? content : "";
            }
            
            throw new RuntimeException("Chat API返回结果为空");

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用Chat API失败", e);
            throw new RuntimeException("调用Chat API失败: " + e.getMessage(), e);
        }
    }
}