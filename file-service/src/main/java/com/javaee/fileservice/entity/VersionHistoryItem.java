package com.javaee.fileservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本历史条目 DTO (v3.0)
 * 用于解析 version_history JSON 字段中的单个版本记录
 */
public class VersionHistoryItem {

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 该版本的文件UUID（物理文件标识）
     */
    @JsonProperty("file_uuid")
    private String fileUuid;

    /**
     * 该版本的原始文件名
     */
    @JsonProperty("original_name")
    private String originalName;

    /**
     * 该版本的物理存储路径（MinIO）
     * 用于下载历史版本物理文件
     */
    @JsonProperty("storage_path")
    private String storagePath;

    /**
     * 该版本的文件大小(bytes)
     */
    @JsonProperty("file_size")
    private Long fileSize;

    /**
     * 该版本的AI摘要
     */
    private String summary;

    /**
     * 该版本的AI关键词
     */
    private List<String> keywords;

    /**
     * 该版本的AI分类
     */
    private String category;

    /**
     * 变更说明
     */
    @JsonProperty("change_summary")
    private String changeSummary;

    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFileUuid() {
        return fileUuid;
    }

    public void setFileUuid(String fileUuid) {
        this.fileUuid = fileUuid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 转换为JSON字符串（用于序列化）
     */
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"version\":").append(version).append(",");
        sb.append("\"file_uuid\":\"").append(fileUuid != null ? fileUuid : "").append("\",");
        sb.append("\"original_name\":\"").append(originalName != null ? escapeJson(originalName) : "").append("\",");
        sb.append("\"storage_path\":\"").append(storagePath != null ? escapeJson(storagePath) : "").append("\",");
        sb.append("\"file_size\":").append(fileSize != null ? fileSize : 0).append(",");
        sb.append("\"summary\":\"").append(summary != null ? escapeJson(summary) : "").append("\",");
        sb.append("\"keywords\":").append(keywordsToJson()).append(",");
        sb.append("\"category\":\"").append(category != null ? category : "其他").append("\",");
        sb.append("\"change_summary\":\"").append(changeSummary != null ? escapeJson(changeSummary) : "").append("\",");
        sb.append("\"create_time\":\"").append(createTime != null ? createTime.toString() : "").append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 关键词列表转JSON数组字符串
     */
    private String keywordsToJson() {
        if (keywords == null || keywords.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(keywords.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * JSON字符串转义
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}