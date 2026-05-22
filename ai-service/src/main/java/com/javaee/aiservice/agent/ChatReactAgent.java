package com.javaee.aiservice.agent;

import com.javaee.aiservice.conversation.ConversationManager;
import com.javaee.aiservice.conversation.ContextManager;
import com.javaee.aiservice.rag.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Chat ReAct Agent
 * 基于ReAct模式实现对话功能
 * 支持多轮对话和上下文管理
 * 支持流式输出
 */
@Component
public class ChatReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ChatReactAgent.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private PromptEngineeringService promptEngineeringService;

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private KnowledgeBase knowledgeBase;

    /**
     * 处理对话请求（同步）
     */
    public Map<String, Object> chat(String conversationId, String userInput, Map<String, Object> context) {
        log.info("处理对话请求: conversationId={}, userInput={}", conversationId, userInput);

        try {
            List<String> history = conversationManager.getConversationHistory(conversationId);
            String knowledgeContext = retrieveKnowledge(userInput);

            String answer = chatService.callChatApi(
                promptEngineeringService.createQAPrompt(userInput, knowledgeContext, history)
            );

            conversationManager.addMessage(conversationId, userInput, answer);
            contextManager.updateContext(conversationId, context);

            return Map.of(
                "status", "success",
                "conversationId", conversationId,
                "answer", answer,
                "context", contextManager.getContext(conversationId)
            );
        } catch (Exception e) {
            log.error("对话处理失败", e);
            return Map.of(
                "status", "error",
                "message", "对话处理失败: " + e.getMessage()
            );
        }
    }

    /**
     * 处理对话请求（流式）
     * 使用回调方式处理流式响应
     * @param conversationId 对话ID
     * @param userInput 用户输入
     * @param context 上下文信息
     * @param onChunk 每个chunk的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChat(String conversationId, String userInput, Map<String, Object> context,
                          Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError) {
        log.info("处理流式对话请求: conversationId={}, userInput={}", conversationId, userInput);

        try {
            List<String> history = conversationManager.getConversationHistory(conversationId);
            String knowledgeContext = retrieveKnowledge(userInput);

            String prompt = promptEngineeringService.createQAPrompt(userInput, knowledgeContext, history);

            // 收集完整响应
            AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

            chatService.streamChatApi(
                prompt,
                chunk -> {
                    fullResponse.get().append(chunk);
                    onChunk.accept(chunk);
                },
                () -> {
                    // 流式结束后保存完整对话历史
                    String answer = fullResponse.get().toString();
                    conversationManager.addMessage(conversationId, userInput, answer);
                    contextManager.updateContext(conversationId, context);
                    log.info("流式对话完成: conversationId={}, responseLength={}", conversationId, answer.length());
                    onComplete.run();
                },
                error -> {
                    log.error("流式对话失败: conversationId={}, error={}", conversationId, error);
                    onError.accept(error);
                }
            );

        } catch (Exception e) {
            log.error("流式对话处理失败", e);
            onError.accept(e.getMessage());
        }
    }

    /**
     * 开始新对话
     */
    public String startConversation(String userId) {
        String conversationId = conversationManager.createConversation(userId);
        log.info("创建新对话: userId={}, conversationId={}", userId, conversationId);
        return conversationId;
    }

    /**
     * 结束对话
     */
    public boolean endConversation(String conversationId) {
        conversationManager.deleteConversation(conversationId);
        contextManager.clearContext(conversationId);
        log.info("结束对话: conversationId={}", conversationId);
        return true;
    }

    /**
     * 获取对话历史
     */
    public List<String> getHistory(String conversationId) {
        return conversationManager.getConversationHistory(conversationId);
    }

    /**
     * 检索知识库
     */
    private String retrieveKnowledge(String query) {
        try {
            List<Map<String, Object>> results = knowledgeBase.search(query, 3);
            StringBuilder context = new StringBuilder();
            for (Map<String, Object> result : results) {
                context.append(result.get("content")).append("\n");
            }
            return context.toString();
        } catch (Exception e) {
            log.warn("知识库检索失败", e);
            return "";
        }
    }
}