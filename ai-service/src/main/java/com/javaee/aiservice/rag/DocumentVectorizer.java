package com.javaee.aiservice.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文档向量化器
 * 负责将文档内容转换为向量表示
 * 适配阿里云百炼dashscope的Embedding API
 */
@Component
public class DocumentVectorizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentVectorizer.class);

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.embedding.model:text-embedding-v3}")
    private String model;

    @Value("${spring.ai.dashscope.embedding.dimension:1024}")
    private int dimension;

    /**
     * 检查API配置是否可用
     * @return 是否配置了dashscope API
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_dashscope_api_key_here");
    }

    /**
     * 将文本向量化
     * @param text 文本内容
     * @return 向量表示
     */
    public float[] vectorize(String text) {
        log.debug("开始向量化文本，长度={}", text.length());

        // 检查API配置
        if (!isConfigured()) {
            log.warn("Dashscope API未配置，使用模拟向量");
            return generateMockVector(text);
        }

        try {
            // 构建请求参数
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .texts(Arrays.asList(text))
                    .build();

            // 创建模型实例并调用
            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            log.debug("Embedding API调用成功");

            // 解析结果
            if (result != null && result.getOutput() != null
                    && result.getOutput().getEmbeddings() != null
                    && !result.getOutput().getEmbeddings().isEmpty()) {

                List<Double> embeddingList = result.getOutput().getEmbeddings().get(0).getEmbedding();
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }

                log.debug("解析到向量维度: {}", embedding.length);
                return embedding;
            }

            throw new RuntimeException("Embedding API返回结果为空");

        } catch (ApiException | NoApiKeyException e) {
            log.error("调用Embedding API失败", e);
            // 使用模拟向量作为fallback
            log.warn("使用模拟向量作为fallback");
            return generateMockVector(text);
        }
    }

    /**
     * 生成模拟向量（当API不可用时的fallback）
     * 基于文本内容的简单哈希生成向量，用于测试和fallback场景
     * @param text 文本内容
     * @return 模拟向量
     */
    private float[] generateMockVector(String text) {
        float[] vector = new float[dimension];

        // 使用文本内容的哈希值生成向量
        int hash = text.hashCode();
        for (int i = 0; i < dimension; i++) {
            // 生成归一化的随机向量
            vector[i] = (float) ((hash * (i + 1) % 1000) / 1000.0 - 0.5);
        }

        // 归一化向量
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量列表
     */
    public float[][] vectorizeBatch(String[] texts) {
        log.info("批量向量化，数量={}", texts.length);

        try {
            float[][] result = new float[texts.length][];
            for (int i = 0; i < texts.length; i++) {
                result[i] = vectorize(texts[i]);
            }
            log.info("批量向量化完成");
            return result;
        } catch (Exception e) {
            log.error("批量向量化失败", e);
            throw new RuntimeException("批量向量化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取向量维度
     * @return 向量维度
     */
    public int getVectorDimension() {
        try {
            float[] sample = vectorize("test");
            return sample.length;
        } catch (Exception e) {
            log.warn("获取向量维度失败，使用默认值", e);
            return dimension;
        }
    }
}