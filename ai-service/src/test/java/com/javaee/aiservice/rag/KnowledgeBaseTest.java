package com.javaee.aiservice.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * KnowledgeBase单元测试 (v3.0)
 * 验证知识库软删除、恢复文档功能
 *
 * 验收标准：
 * 1. softDeleteDocument同时标记文档和向量deleted=true
 * 2. restoreDocument同时恢复文档和向量deleted=false
 * 3. updateDocument使用软删除策略而非物理删除
 */
class KnowledgeBaseTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DocumentVectorizer vectorizer;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Reranker reranker;

    @InjectMocks
    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试1：软删除文档
     */
    @Test
    @DisplayName("softDeleteDocument同时标记文档和向量deleted=true")
    void testSoftDeleteDocument() {
        // 模拟Hash操作 - 使用正确的泛型类型
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 执行软删除
        knowledgeBase.softDeleteDocument("test-doc-1");

        // 验证文档元数据标记deleted=true
        verify(hashOps).put("doc:test-doc-1", "deleted", true);

        // 验证向量软删除
        verify(vectorStore).softDelete("test-doc-1");
    }

    /**
     * 测试2：恢复文档
     */
    @Test
    @DisplayName("restoreDocument同时恢复文档和向量deleted=false")
    void testRestoreDocument() {
        // 模拟Hash操作
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 执行恢复
        knowledgeBase.restoreDocument("test-doc-1");

        // 验证文档元数据标记deleted=false
        verify(hashOps).put("doc:test-doc-1", "deleted", false);

        // 验证向量恢复
        verify(vectorStore).restore("test-doc-1");
    }

    /**
     * 测试3：更新文档使用软删除策略
     */
    @Test
    @DisplayName("updateDocument使用软删除策略，保留历史版本向量")
    void testUpdateDocumentWithSoftDelete() {
        // 模拟Hash操作
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 模拟ValueOperations
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        // 模拟当前版本号
        when(hashOps.get("doc:test-doc-1", "version")).thenReturn(2);

        // 模拟向量化
        float[] mockVector = new float[]{0.1f, 0.2f, 0.3f};
        when(vectorizer.vectorize(anyString())).thenReturn(mockVector);

        // 执行更新
        String newContent = "这是更新后的内容";
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("title", "更新后的文档");
        newMetadata.put("userId", "1");

        knowledgeBase.updateDocument("test-doc-1", newContent, newMetadata);

        // 验证：不应该调用removeDocument（物理删除）
        verify(redisTemplate, never()).delete("doc:test-doc-1");
        verify(stringRedisTemplate, never()).delete("content:test-doc-1");
        verify(vectorStore, never()).delete("test-doc-1");

        // 验证：应该调用向量软删除
        verify(vectorStore).softDelete("test-doc-1");

        // 验证：应该存储新版本向量（ID为test-doc-1_v3）
        verify(vectorStore).store(eq("test-doc-1_v3"), any(float[].class), any(Map.class));

        // 验证：新版本version字段为3
        assertEquals(3, newMetadata.get("version"), "新版本version应为3");
    }

    /**
     * 测试4：添加文档时自动存储向量
     */
    @Test
    @DisplayName("addDocument自动生成向量并存储")
    void testAddDocument() {
        // 模拟Value和Hash操作
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 模拟向量化
        float[] mockVector = new float[]{0.1f, 0.2f, 0.3f};
        when(vectorizer.vectorize(anyString())).thenReturn(mockVector);

        // 执行添加
        String content = "这是文档内容";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "测试文档");
        metadata.put("userId", "1");

        knowledgeBase.addDocument("test-doc-1", content, metadata);

        // 验证内容存储
        verify(valueOps).set("content:test-doc-1", content);

        // 验证元数据存储
        verify(hashOps).putAll("doc:test-doc-1", metadata);

        // 验证向量生成和存储
        verify(vectorizer).vectorize(content);
        verify(vectorStore).store(eq("test-doc-1"), any(float[].class), any(Map.class));
    }

    /**
     * 测试5：物理删除文档
     */
    @Test
    @DisplayName("removeDocument物理删除文档和向量")
    void testRemoveDocument() {
        // 执行物理删除
        knowledgeBase.removeDocument("test-doc-1");

        // 验证物理删除文档元数据
        verify(redisTemplate).delete("doc:test-doc-1");

        // 验证物理删除内容
        verify(stringRedisTemplate).delete("content:test-doc-1");

        // 验证物理删除向量
        verify(vectorStore).delete("test-doc-1");
    }
}