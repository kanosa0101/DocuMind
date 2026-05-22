package com.javaee.aiservice.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 向量存储 (v3.0增强)
 * 使用Redis实现向量存储和检索
 * 支持近似最近邻搜索、软删除、版本管理
 */
@Component
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);
    private static final String VECTOR_PREFIX = "vector:";
    private static final String METADATA_PREFIX = "metadata:";
    private static final String DELETED_FIELD = "deleted";
    private static final String VERSION_FIELD = "version";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 存储向量 (v3.0增强)
     * @param id 文档ID
     * @param vector 向量
     * @param metadata 元数据（自动添加deleted=false，version默认1）
     */
    public void store(String id, float[] vector, Map<String, Object> metadata) {
        log.info("存储向量: id={}, dimension={}", id, vector.length);

        try {
            String vectorKey = VECTOR_PREFIX + id;
            String metadataKey = METADATA_PREFIX + id;

            // v3.0：自动添加deleted和version字段
            metadata.put(DELETED_FIELD, false);
            if (!metadata.containsKey(VERSION_FIELD)) {
                metadata.put(VERSION_FIELD, 1);
            }

            // 使用StringRedisTemplate存储向量JSON字符串
            String vectorJson = objectMapper.writeValueAsString(toList(vector));
            stringRedisTemplate.opsForValue().set(vectorKey, vectorJson);

            // 使用RedisTemplate存储元数据
            redisTemplate.opsForHash().putAll(metadataKey, metadata);

            log.info("向量存储成功: id={}, version={}", id, metadata.get(VERSION_FIELD));
        } catch (Exception e) {
            log.error("向量存储失败", e);
            throw new RuntimeException("向量存储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将float[]转换为List<Float>
     */
    private List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>();
        for (float v : vector) {
            list.add(v);
        }
        return list;
    }

    /**
     * 搜索相似向量 (v3.0增强)
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @return 搜索结果列表（过滤deleted=true的向量）
     */
    public List<Map<String, Object>> search(float[] queryVector, int topK) {
        log.info("搜索相似向量: topK={}", topK);

        try {
            Set<String> keys = stringRedisTemplate.keys(VECTOR_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                log.warn("没有找到任何向量数据");
                return Collections.emptyList();
            }

            List<SearchResult> results = new ArrayList<>();

            for (String key : keys) {
                String vectorJson = stringRedisTemplate.opsForValue().get(key);
                if (vectorJson != null) {
                    float[] storedVector = parseVectorJson(vectorJson);
                    if (storedVector != null) {
                        String id = key.substring(VECTOR_PREFIX.length());

                        // v3.0：检查deleted标记，过滤已删除向量
                        Map<String, Object> metadata = getMetadata(id);
                        Boolean deleted = (Boolean) metadata.get(DELETED_FIELD);
                        if (deleted != null && deleted) {
                            continue; // 跳过已删除向量
                        }

                        float similarity = cosineSimilarity(queryVector, storedVector);
                        results.add(new SearchResult(id, similarity));
                    }
                }
            }

            results.sort((a, b) -> Float.compare(b.similarity, a.similarity));

            List<Map<String, Object>> finalResults = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, results.size()); i++) {
                SearchResult result = results.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("id", result.id);
                item.put("similarity", result.similarity);
                item.putAll(getMetadata(result.id));
                finalResults.add(item);
            }

            log.info("搜索完成，找到{}个结果", finalResults.size());
            return finalResults;
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON解析向量
     */
    private float[] parseVectorJson(String json) {
        try {
            List<Float> list = objectMapper.readValue(json, new TypeReference<List<Float>>() {});
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = list.get(i);
            }
            return vector;
        } catch (Exception e) {
            log.warn("解析向量JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 删除向量（物理删除）
     * @param id 文档ID
     */
    public void delete(String id) {
        log.info("物理删除向量: id={}", id);

        try {
            stringRedisTemplate.delete(VECTOR_PREFIX + id);
            redisTemplate.delete(METADATA_PREFIX + id);
            log.info("向量物理删除成功: id={}", id);
        } catch (Exception e) {
            log.error("向量删除失败", e);
            throw new RuntimeException("向量删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 软删除向量 (v3.0新增)
     * 标记向量deleted=true，不物理删除，搜索时过滤
     * @param id 文档ID
     */
    public void softDelete(String id) {
        log.info("软删除向量: id={}", id);

        try {
            String metadataKey = METADATA_PREFIX + id;
            redisTemplate.opsForHash().put(metadataKey, DELETED_FIELD, true);
            log.info("向量软删除成功: id={}", id);
        } catch (Exception e) {
            log.error("向量软删除失败", e);
            throw new RuntimeException("向量软删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复向量 (v3.0新增)
     * 标记向量deleted=false，恢复可检索状态
     * @param id 文档ID
     */
    public void restore(String id) {
        log.info("恢复向量: id={}", id);

        try {
            String metadataKey = METADATA_PREFIX + id;
            redisTemplate.opsForHash().put(metadataKey, DELETED_FIELD, false);
            log.info("向量恢复成功: id={}", id);
        } catch (Exception e) {
            log.error("向量恢复失败", e);
            throw new RuntimeException("向量恢复失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新向量版本 (v3.0新增)
     * 版本更新时：旧向量软删除，新向量创建
     * @param id 文档ID
     * @param vector 新向量
     * @param metadata 新元数据
     * @param newVersion 新版本号
     */
    public void updateVersion(String id, float[] vector, Map<String, Object> metadata, int newVersion) {
        log.info("更新向量版本: id={}, newVersion={}", id, newVersion);

        try {
            // 生成新版本ID：fileUuid_v版本号
            String newId = id + "_v" + newVersion;

            // 旧向量软删除
            softDelete(id);

            // 新向量存储
            metadata.put(VERSION_FIELD, newVersion);
            store(newId, vector, metadata);

            log.info("向量版本更新成功: oldId={}, newId={}", id, newId);
        } catch (Exception e) {
            log.error("向量版本更新失败", e);
            throw new RuntimeException("向量版本更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 切换版本向量 (v3.0新增)
     * 恢复目标版本向量为可检索状态
     * @param id 文档ID基础部分（不含版本后缀）
     * @param targetVersion 目标版本号
     */
    public void switchVersion(String id, int targetVersion) {
        log.info("切换向量版本: id={}, targetVersion={}", id, targetVersion);

        try {
            // 当前版本ID
            Map<String, Object> currentMetadata = getMetadata(id);
            if (currentMetadata != null && !currentMetadata.isEmpty()) {
                // 当前向量软删除
                softDelete(id);
            }

            // 目标版本ID
            String targetId = id + "_v" + targetVersion;
            Map<String, Object> targetMetadata = getMetadata(targetId);

            if (targetMetadata != null && !targetMetadata.isEmpty()) {
                // 目标版本向量恢复
                restore(targetId);
                log.info("向量版本切换成功: 恢复{}", targetId);
            } else {
                log.warn("目标版本向量不存在: {}", targetId);
            }
        } catch (Exception e) {
            log.error("向量版本切换失败", e);
            throw new RuntimeException("向量版本切换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取元数据
     */
    private Map<String, Object> getMetadata(String id) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(METADATA_PREFIX + id);
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
            return metadata;
        } catch (Exception e) {
            log.warn("获取元数据失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0f;
        }

        return dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 搜索结果内部类
     */
    private static class SearchResult {
        String id;
        float similarity;

        SearchResult(String id, float similarity) {
            this.id = id;
            this.similarity = similarity;
        }
    }
}