package com.javaee.fileservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.fileservice.entity.VersionHistoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 版本历史JSON解析工具类 (v3.0)
 * 用于解析和构建 file_info.version_history JSON字段
 */
public class VersionHistoryParser {

    private static final Logger log = LoggerFactory.getLogger(VersionHistoryParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 解析version_history JSON字符串为列表
     *
     * @param versionHistoryJson JSON字符串
     * @return 版本历史列表（可修改），如果解析失败返回空列表
     */
    public static List<VersionHistoryItem> parse(String versionHistoryJson) {
        if (versionHistoryJson == null || versionHistoryJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            objectMapper.findAndRegisterModules();
            List<VersionHistoryItem> result = objectMapper.readValue(versionHistoryJson, new TypeReference<List<VersionHistoryItem>>() {});
            return new ArrayList<>(result);  // 返回可修改的列表
        } catch (JsonProcessingException e) {
            log.warn("解析版本历史JSON失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将版本历史列表转换为JSON字符串
     *
     * @param historyList 版本历史列表
     * @return JSON字符串
     */
    public static String toJson(List<VersionHistoryItem> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return "[]";
        }

        try {
            objectMapper.findAndRegisterModules();
            return objectMapper.writeValueAsString(historyList);
        } catch (JsonProcessingException e) {
            log.error("序列化版本历史失败: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * 添加新版本到历史记录
     *
     * @param existingHistoryJson 现有的历史JSON
     * @param newItem 新版本条目
     * @return 更新后的JSON字符串
     */
    public static String addVersion(String existingHistoryJson, VersionHistoryItem newItem) {
        List<VersionHistoryItem> history = parse(existingHistoryJson);
        List<VersionHistoryItem> mutableHistory = new ArrayList<>(history);
        mutableHistory.add(newItem);
        return toJson(mutableHistory);
    }

    /**
     * 构建版本历史条目
     *
     * @param version 版本号
     * @param fileUuid 文件UUID
     * @param originalName 原始文件名
     * @param storagePath 存储路径
     * @param fileSize 文件大小
     * @param summary 摘要
     * @param keywords 关键词列表
     * @param category 分类
     * @param changeSummary 变更说明
     * @param createTime 创建时间
     * @return 版本历史条目对象
     */
    public static VersionHistoryItem buildItem(
            Integer version,
            String fileUuid,
            String originalName,
            String storagePath,
            Long fileSize,
            String summary,
            List<String> keywords,
            String category,
            String changeSummary,
            LocalDateTime createTime) {

        VersionHistoryItem item = new VersionHistoryItem();
        item.setVersion(version);
        item.setFileUuid(fileUuid);
        item.setOriginalName(originalName);
        item.setStoragePath(storagePath);
        item.setFileSize(fileSize);
        item.setSummary(summary);
        item.setKeywords(keywords);
        item.setCategory(category);
        item.setChangeSummary(changeSummary);
        item.setCreateTime(createTime);
        return item;
    }

    /**
     * 获取指定版本的历史条目
     *
     * @param versionHistoryJson JSON字符串
     * @param targetVersion 目标版本号
     * @return 版本条目，不存在则返回null
     */
    public static VersionHistoryItem getVersion(String versionHistoryJson, Integer targetVersion) {
        List<VersionHistoryItem> history = parse(versionHistoryJson);
        for (VersionHistoryItem item : history) {
            if (item.getVersion().equals(targetVersion)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取最新版本号（不包括当前版本）
     *
     * @param versionHistoryJson JSON字符串
     * @return 最新历史版本号，没有历史则返回null
     */
    public static Integer getLatestHistoryVersion(String versionHistoryJson) {
        List<VersionHistoryItem> history = parse(versionHistoryJson);
        if (history.isEmpty()) {
            return null;
        }
        // 按版本号降序排序
        history.sort((a, b) -> b.getVersion().compareTo(a.getVersion()));
        return history.get(0).getVersion();
    }

    /**
     * 计算版本历史数量
     *
     * @param versionHistoryJson JSON字符串
     * @return 版本数量
     */
    public static int countVersions(String versionHistoryJson) {
        return parse(versionHistoryJson).size();
    }

    /**
     * 解析keywords JSON字符串为列表
     *
     * @param keywordsJson JSON字符串
     * @return 关键词列表（可修改）
     */
    public static List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            objectMapper.findAndRegisterModules();
            List<String> result = objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {});
            return new ArrayList<>(result);
        } catch (JsonProcessingException e) {
            // 尝试逗号分隔格式
            if (keywordsJson.contains(",")) {
                String[] parts = keywordsJson.split(",");
                List<String> keywords = new ArrayList<>();
                for (String part : parts) {
                    keywords.add(part.trim());
                }
                return keywords;
            }
            // 单个关键词
            List<String> single = new ArrayList<>();
            single.add(keywordsJson.trim());
            return single;
        }
    }

    /**
     * 将关键词列表转换为JSON字符串
     *
     * @param keywords 关键词列表
     * @return JSON字符串
     */
    public static String keywordsToJson(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            log.error("序列化关键词失败: {}", e.getMessage());
            return "[]";
        }
    }
}