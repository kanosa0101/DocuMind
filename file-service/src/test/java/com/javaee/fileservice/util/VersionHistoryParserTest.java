package com.javaee.fileservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.fileservice.entity.VersionHistoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VersionHistoryParser单元测试
 * Phase 1验收标准：所有测试通过
 */
class VersionHistoryParserTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // ===== 测试JSON解析 =====

    @Test
    @DisplayName("解析空JSON字符串应返回空列表")
    void testParseEmptyJson() {
        List<VersionHistoryItem> result = VersionHistoryParser.parse(null);
        assertEquals(0, result.size());

        result = VersionHistoryParser.parse("");
        assertEquals(0, result.size());

        result = VersionHistoryParser.parse("[]");
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("解析有效JSON应返回正确列表")
    void testParseValidJson() {
        String json = "[{\"version\":1,\"file_uuid\":\"uuid-1\",\"original_name\":\"test.pdf\",\"storage_path\":\"minio/path\",\"summary\":\"v1摘要\",\"keywords\":[\"AI\",\"深度学习\"],\"change_summary\":\"初始版本\",\"create_time\":\"2026-05-17T10:00:00\"}]";

        List<VersionHistoryItem> result = VersionHistoryParser.parse(json);

        assertEquals(1, result.size());
        VersionHistoryItem item = result.get(0);
        assertEquals(1, item.getVersion());
        assertEquals("uuid-1", item.getFileUuid());
        assertEquals("test.pdf", item.getOriginalName());
        assertEquals("v1摘要", item.getSummary());
        assertEquals(2, item.getKeywords().size());
        assertTrue(item.getKeywords().contains("AI"));
    }

    @Test
    @DisplayName("解析多个版本JSON应返回完整列表")
    void testParseMultipleVersions() {
        String json = """
            [
                {"version":1,"file_uuid":"uuid-1","original_name":"v1.pdf","create_time":"2026-05-17T10:00:00"},
                {"version":2,"file_uuid":"uuid-2","original_name":"v2.pdf","create_time":"2026-05-17T11:00:00"},
                {"version":3,"file_uuid":"uuid-3","original_name":"v3.pdf","create_time":"2026-05-17T12:00:00"}
            ]
            """;

        List<VersionHistoryItem> result = VersionHistoryParser.parse(json);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getVersion());
        assertEquals(2, result.get(1).getVersion());
        assertEquals(3, result.get(2).getVersion());
    }

    @Test
    @DisplayName("解析无效JSON应返回空列表并记录警告")
    void testParseInvalidJson() {
        String invalidJson = "invalid json string";
        List<VersionHistoryItem> result = VersionHistoryParser.parse(invalidJson);
        assertEquals(0, result.size());
    }

    // ===== 测试JSON序列化 =====

    @Test
    @DisplayName("空列表序列化应返回空数组JSON")
    void testToJsonEmptyList() {
        String json = VersionHistoryParser.toJson(null);
        assertEquals("[]", json);

        json = VersionHistoryParser.toJson(new ArrayList<>());
        assertEquals("[]", json);
    }

    @Test
    @DisplayName("列表序列化应返回有效JSON")
    void testToJsonValidList() {
        List<VersionHistoryItem> list = new ArrayList<>();
        VersionHistoryItem item = VersionHistoryParser.buildItem(
                1, "uuid-1", "test.pdf", "minio/path",
                1024L, "摘要", Arrays.asList("AI", "测试"), "论文",
                "初始版本", LocalDateTime.of(2026, 5, 17, 10, 0)
        );
        list.add(item);

        String json = VersionHistoryParser.toJson(list);

        assertNotNull(json);
        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"file_uuid\":\"uuid-1\""));
        assertTrue(json.contains("AI"));
    }

    // ===== 测试添加版本 =====

    @Test
    @DisplayName("向空历史添加新版本")
    void testAddVersionToEmptyHistory() {
        VersionHistoryItem newItem = new VersionHistoryItem();
        newItem.setVersion(1);
        newItem.setFileUuid("uuid-1");
        newItem.setOriginalName("v1.pdf");

        String result = VersionHistoryParser.addVersion(null, newItem);

        List<VersionHistoryItem> parsed = VersionHistoryParser.parse(result);
        assertEquals(1, parsed.size());
        assertEquals(1, parsed.get(0).getVersion());
    }

    @Test
    @DisplayName("向已有历史添加新版本")
    void testAddVersionToExistingHistory() {
        String existingJson = "[{\"version\":1,\"file_uuid\":\"uuid-1\"}]";

        VersionHistoryItem newItem = new VersionHistoryItem();
        newItem.setVersion(2);
        newItem.setFileUuid("uuid-2");

        String result = VersionHistoryParser.addVersion(existingJson, newItem);

        List<VersionHistoryItem> parsed = VersionHistoryParser.parse(result);
        assertEquals(2, parsed.size());
    }

    // ===== 测试获取版本 =====

    @Test
    @DisplayName("获取指定版本应返回正确条目")
    void testGetVersion() {
        String json = "[{\"version\":1,\"summary\":\"v1\"},{\"version\":2,\"summary\":\"v2\"}]";

        VersionHistoryItem item = VersionHistoryParser.getVersion(json, 2);

        assertNotNull(item);
        assertEquals(2, item.getVersion());
        assertEquals("v2", item.getSummary());
    }

    @Test
    @DisplayName("获取不存在版本应返回null")
    void testGetVersionNotFound() {
        String json = "[{\"version\":1}]";

        VersionHistoryItem item = VersionHistoryParser.getVersion(json, 999);

        assertNull(item);
    }

    // ===== 测试关键词解析 =====

    @Test
    @DisplayName("解析JSON格式关键词")
    void testParseKeywordsJson() {
        String json = "[\"AI\",\"深度学习\",\"神经网络\"]";
        List<String> keywords = VersionHistoryParser.parseKeywords(json);

        assertEquals(3, keywords.size());
        assertTrue(keywords.contains("AI"));
        assertTrue(keywords.contains("深度学习"));
    }

    @Test
    @DisplayName("解析逗号分隔关键词")
    void testParseKeywordsCommaSeparated() {
        String commaStr = "AI,深度学习,神经网络";
        List<String> keywords = VersionHistoryParser.parseKeywords(commaStr);

        assertEquals(3, keywords.size());
        assertEquals("AI", keywords.get(0));
        assertEquals("深度学习", keywords.get(1));
    }

    @Test
    @DisplayName("解析单个关键词")
    void testParseKeywordsSingle() {
        String single = "AI";
        List<String> keywords = VersionHistoryParser.parseKeywords(single);

        assertEquals(1, keywords.size());
        assertEquals("AI", keywords.get(0));
    }

    @Test
    @DisplayName("关键词序列化")
    void testKeywordsToJson() {
        List<String> keywords = Arrays.asList("AI", "深度学习");
        String json = VersionHistoryParser.keywordsToJson(keywords);

        assertEquals("[\"AI\",\"深度学习\"]", json);
    }

    // ===== 测试构建条目 =====

    @Test
    @DisplayName("buildItem应创建完整条目")
    void testBuildItem() {
        LocalDateTime createTime = LocalDateTime.of(2026, 5, 17, 10, 0);
        VersionHistoryItem item = VersionHistoryParser.buildItem(
                1, "uuid-1", "test.pdf", "minio/path",
                1024L, "摘要内容", Arrays.asList("AI", "测试"), "论文",
                "初始版本", createTime
        );

        assertEquals(1, item.getVersion());
        assertEquals("uuid-1", item.getFileUuid());
        assertEquals("test.pdf", item.getOriginalName());
        assertEquals("minio/path", item.getStoragePath());
        assertEquals(1024L, item.getFileSize());
        assertEquals("摘要内容", item.getSummary());
        assertEquals(2, item.getKeywords().size());
        assertEquals("论文", item.getCategory());
        assertEquals("初始版本", item.getChangeSummary());
        assertEquals(createTime, item.getCreateTime());
    }

    // ===== 测试版本统计 =====

    @Test
    @DisplayName("统计版本数量")
    void testCountVersions() {
        String json = "[{\"version\":1},{\"version\":2},{\"version\":3}]";
        int count = VersionHistoryParser.countVersions(json);
        assertEquals(3, count);

        count = VersionHistoryParser.countVersions(null);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("获取最新历史版本号")
    void testGetLatestHistoryVersion() {
        String json = "[{\"version\":1},{\"version\":3},{\"version\":2}]";
        Integer latest = VersionHistoryParser.getLatestHistoryVersion(json);

        assertEquals(3, latest);

        latest = VersionHistoryParser.getLatestHistoryVersion(null);
        assertNull(latest);
    }

    // ===== 性能测试 =====

    @Test
    @DisplayName("解析100版本JSON应在100ms内完成")
    void testPerformance100Versions() {
        // 构建100版本JSON
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= 100; i++) {
            if (i > 1) sb.append(",");
            sb.append("{\"version\":").append(i).append(",\"file_uuid\":\"uuid-").append(i).append("\"}");
        }
        sb.append("]");
        String json = sb.toString();

        long startTime = System.currentTimeMillis();
        List<VersionHistoryItem> result = VersionHistoryParser.parse(json);
        long endTime = System.currentTimeMillis();

        assertEquals(100, result.size());
        assertTrue((endTime - startTime) < 100, "解析100版本应<100ms，实际耗时: " + (endTime - startTime) + "ms");
    }
}