package com.javaee.fileservice.dto;

import java.util.Map;

/**
 * 文件统计DTO (v3.0)
 */
public class FileStatsDTO {

    /**
     * 总文件数
     */
    private Long totalFiles;

    /**
     * 活跃文件数
     */
    private Long activeFiles;

    /**
     * 已删除文件数
     */
    private Long deletedFiles;

    /**
     * 总存储大小(bytes)
     */
    private Long totalSize;

    /**
     * 已索引文件数
     */
    private Long indexedFiles;

    /**
     * 各分类文件数
     */
    private Map<String, Long> categoryCounts;

    /**
     * 各类型文件数
     */
    private Map<String, Long> typeCounts;

    /**
     * 多版本文件数
     */
    private Long multiVersionFiles;

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Long getActiveFiles() {
        return activeFiles;
    }

    public void setActiveFiles(Long activeFiles) {
        this.activeFiles = activeFiles;
    }

    public Long getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(Long deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getIndexedFiles() {
        return indexedFiles;
    }

    public void setIndexedFiles(Long indexedFiles) {
        this.indexedFiles = indexedFiles;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public Map<String, Long> getTypeCounts() {
        return typeCounts;
    }

    public void setTypeCounts(Map<String, Long> typeCounts) {
        this.typeCounts = typeCounts;
    }

    public Long getMultiVersionFiles() {
        return multiVersionFiles;
    }

    public void setMultiVersionFiles(Long multiVersionFiles) {
        this.multiVersionFiles = multiVersionFiles;
    }
}