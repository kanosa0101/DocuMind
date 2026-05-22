package com.javaee.aiservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Chat服务
 * 支持DeepSeek OpenAI兼容API
 * 支持同步调用和流式输出
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用Chat API（同步）
     * @param prompt 用户提示词
     * @return 响应内容
     */
    public String callChatApi(String prompt) {
        log.info("调用Chat API: model={}, prompt length={}", model, prompt.length());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key未配置");
            return "AI服务未正确配置，请检查环境变量 OPENAI_API_KEY";
        }

        try {
            URL url = new URL(baseUrl + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            // 构建请求体
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            String requestBody = objectMapper.writeValueAsString(Map.of("model", model, "messages", messages));
            conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

            // 读取响应
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonNode root = objectMapper.readTree(response.toString());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode content = choices.get(0).path("message").path("content");
                    String result = content.asText("");
                    log.info("Chat响应长度: {}", result.length());
                    return result;
                }
            }

            log.warn("Chat API返回空响应");
            return "";

        } catch (Exception e) {
            log.error("调用Chat API失败", e);
            throw new RuntimeException("调用Chat API失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用Chat API（流式）
     * 使用HttpURLConnection直接读取SSE流
     * @param prompt 用户提示词
     * @param onChunk 每个chunk的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChatApi(String prompt, Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError) {
        log.info("调用Chat API (流式): model={}, prompt length={}", model, prompt.length());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key未配置");
            onError.accept("AI服务未正确配置");
            return;
        }

        try {
            URL url = new URL(baseUrl + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            // 构建请求体
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", model);
            requestBodyMap.put("messages", messages);
            requestBodyMap.put("stream", true);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

            // 读取SSE流
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String chunk = parseStreamChunk(line);
                    if (chunk != null && !chunk.isEmpty()) {
                        onChunk.accept(chunk);
                    }
                }
                reader.close();
                onComplete.run();
                log.info("流式调用完成");
            } else {
                String error = "HTTP错误: " + conn.getResponseCode();
                log.error(error);
                onError.accept(error);
            }

        } catch (Exception e) {
            log.error("流式调用失败", e);
            onError.accept(e.getMessage());
        }
    }

    /**
     * 解析SSE流式chunk
     * @param line SSE行数据
     * @return 解析后的文本内容
     */
    private String parseStreamChunk(String line) {
        try {
            // 跳过空行
            if (line == null || line.isEmpty()) {
                return null;
            }

            // SSE格式: "data: {...}" 或 "data: [DONE]"
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    return null;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode delta = choices.get(0).path("delta");
                    // DeepSeek可能返回content或reasoning_content（思维链模式）
                    JsonNode content = delta.path("content");
                    JsonNode reasoningContent = delta.path("reasoning_content");

                    String text = null;
                    if (!content.isMissingNode() && !content.isNull()) {
                        String contentText = content.asText();
                        if (contentText != null && !contentText.isEmpty()) {
                            text = contentText;
                        }
                    }
                    if (text == null && !reasoningContent.isMissingNode() && !reasoningContent.isNull()) {
                        String reasoningText = reasoningContent.asText();
                        if (reasoningText != null && !reasoningText.isEmpty()) {
                            text = reasoningText;
                        }
                    }

                    return text;
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("解析chunk失败: {}, error: {}", line, e.getMessage());
            return null;
        }
    }
}