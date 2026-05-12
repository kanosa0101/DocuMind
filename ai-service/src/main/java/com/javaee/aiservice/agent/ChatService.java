package com.javaee.aiservice.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Chat服务
 * 支持DeepSeek OpenAI兼容API
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired(required = false)
    private OpenAiChatModel chatModel;

    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String model;

    /**
     * 调用Chat API
     * @param prompt 用户提示词
     * @return 响应内容
     */
    public String callChatApi(String prompt) {
        log.info("调用Chat API: model={}, prompt length={}", model, prompt.length());

        if (chatModel == null) {
            log.warn("ChatModel未配置，返回模拟响应");
            return "AI服务未正确配置，请检查环境变量 OPENAI_API_KEY 和 OPENAI_API_BASE";
        }

        try {
            ChatClient chatClient = ChatClient.create(chatModel);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Chat响应长度: {}", response != null ? response.length() : 0);
            return response != null ? response : "";

        } catch (Exception e) {
            log.error("调用Chat API失败", e);
            throw new RuntimeException("调用Chat API失败: " + e.getMessage(), e);
        }
    }
}