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
 * VectorStore单元测试 (v3.0)
 * 验证向量软删除、恢复、版本管理、搜索过滤功能
 *
 * 验收标准：
 * 1. 软删除后向量deleted=true
 * 2. 恢复后向量deleted=false
 * 3. 搜索过滤deleted=true的向量
 * 4. 存储时自动添加deleted和version字段
 */
class VectorStoreTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试1：存储向量时自动添加deleted和version字段
     */
    @Test
    @DisplayName("存储向量时自动添加deleted=false和version字段")
    void testStoreWithMetadata() {
        // 模拟Redis操作 - 使用正确的泛型类型
        when(stringRedisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "测试文档");

        // 执行存储
        vectorStore.store("test-doc-1", vector, metadata);

        // 验证metadata中包含deleted和version字段
        verify(hashOps).putAll(anyString(), any(Map.class));

        // 验证自动添加的默认值
        assertTrue(metadata.containsKey("deleted"), "metadata应包含deleted字段");
        assertFalse((Boolean) metadata.get("deleted"), "deleted默认应为false");
        assertTrue(metadata.containsKey("version"), "metadata应包含version字段");
        assertEquals(1, metadata.get("version"), "version默认应为1");
    }

    /**
     * 测试2：软删除向量
     */
    @Test
    @DisplayName("软删除向量：标记deleted=true")
    void testSoftDelete() {
        // 模拟Hash操作 - 使用正确的泛型类型
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 执行软删除
        vectorStore.softDelete("test-doc-1");

        // 验证调用了put方法标记deleted=true
        verify(hashOps).put("metadata:test-doc-1", "deleted", true);
    }

    /**
     * 测试3：恢复向量
     */
    @Test
    @DisplayName("恢复向量：标记deleted=false")
    void testRestore() {
        // 模拟Hash操作
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 执行恢复
        vectorStore.restore("test-doc-1");

        // 验证调用了put方法标记deleted=false
        verify(hashOps).put("metadata:test-doc-1", "deleted", false);
    }

    /**
     * 测试4：搜索过滤deleted=true的向量
     */
    @Test
    @DisplayName("搜索时过滤deleted=true的向量")
    void testSearchFiltersDeletedVectors() {
        // 模拟keys方法返回向量ID列表
        Set<String> keys = new HashSet<>();
        keys.add("vector:doc-1");
        keys.add("vector:doc-2");
        keys.add("vector:doc-3");
        when(stringRedisTemplate.keys("vector:*")).thenReturn(keys);

        // 模拟ValueOperations
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        // 模拟向量JSON数据
        String vectorJson = "[0.1, 0.2, 0.3, 0.4, 0.5]";
        when(valueOps.get(anyString())).thenReturn(vectorJson);

        // 模拟HashOperations获取metadata
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // doc-1: deleted=true（应过滤）
        Map<Object, Object> metadata1 = new HashMap<>();
        metadata1.put("deleted", true);
        metadata1.put("version", 1);
        when(hashOps.entries("metadata:doc-1")).thenReturn(metadata1);

        // doc-2: deleted=false（应返回）
        Map<Object, Object> metadata2 = new HashMap<>();
        metadata2.put("deleted", false);
        metadata2.put("version", 1);
        metadata2.put("title", "正常文档");
        when(hashOps.entries("metadata:doc-2")).thenReturn(metadata2);

        // doc-3: deleted=false（应返回）
        Map<Object, Object> metadata3 = new HashMap<>();
        metadata3.put("deleted", false);
        metadata3.put("version", 2);
        when(hashOps.entries("metadata:doc-3")).thenReturn(metadata3);

        // 执行搜索
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        List<Map<String, Object>> results = vectorStore.search(queryVector, 5);

        // 验证结果
        assertNotNull(results, "搜索结果不应为null");

        // 验证过滤了deleted=true的向量（doc-1被过滤）
        for (Map<String, Object> result : results) {
            Boolean deleted = (Boolean) result.get("deleted");
            assertTrue(deleted == null || !deleted, "不应返回deleted=true的向量");
        }
    }

    /**
     * 测试5：版本切换
     */
    @Test
    @DisplayName("版本切换：软删除当前版本，恢复目标版本")
    void testSwitchVersion() {
        // 模拟Hash操作
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // 模拟当前版本metadata
        Map<Object, Object> currentMetadata = new HashMap<>();
        currentMetadata.put("deleted", false);
        currentMetadata.put("version", 3);
        when(hashOps.entries("metadata:test-doc")).thenReturn(currentMetadata);

        // 模拟目标版本metadata（v2）
        Map<Object, Object> targetMetadata = new HashMap<>();
        targetMetadata.put("deleted", true);  // 历史版本已软删除
        targetMetadata.put("version", 2);
        when(hashOps.entries("metadata:test-doc_v2")).thenReturn(targetMetadata);

        // 执行版本切换
        vectorStore.switchVersion("test-doc", 2);

        // 验证当前版本被软删除
        verify(hashOps).put("metadata:test-doc", "deleted", true);

        // 验证目标版本被恢复
        verify(hashOps).put("metadata:test-doc_v2", "deleted", false);
    }

    /**
     * 测试6：版本更新
     */
    @Test
    @DisplayName("版本更新：软删除旧版本，创建新版本向量")
    void testUpdateVersion() {
        // 模拟Hash和Value操作
        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        float[] newVector = new float[]{0.5f, 0.6f, 0.7f};
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("title", "更新后的文档");

        // 执行版本更新
        vectorStore.updateVersion("test-doc", newVector, newMetadata, 3);

        // 验证旧版本被软删除
        verify(hashOps).put("metadata:test-doc", "deleted", true);

        // 验证新版本被存储（ID为test-doc_v3）
        verify(valueOps).set(eq("vector:test-doc_v3"), anyString());
        verify(hashOps).putAll(eq("metadata:test-doc_v3"), any(Map.class));

        // 验证新版本version字段为3
        assertEquals(3, newMetadata.get("version"), "新版本version应为3");
    }
}