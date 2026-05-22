package com.javaee.fileservice.dto;

/**
 * 相似度检测结果DTO（v3.0）
 */
public class SimilarityResultDTO {

    /**
     * 是否检测到相似文件
     */
    private Boolean similarityDetected;

    /**
     * 相似的文件UUID
     */
    private String similarFileUuid;

    /**
     * 相似文件名
     */
    private String similarFileName;

    /**
     * 相似度分数 (0.0 - 1.0)
     */
    private Double similarityScore;

    /**
     * 当前版本号
     */
    private Integer currentVersion;

    /**
     * 推荐操作（NEW / UPDATE_VERSION）
     */
    private String recommendation;

    public Boolean getSimilarityDetected() {
        return similarityDetected;
    }

    public void setSimilarityDetected(Boolean similarityDetected) {
        this.similarityDetected = similarityDetected;
    }

    public String getSimilarFileUuid() {
        return similarFileUuid;
    }

    public void setSimilarFileUuid(String similarFileUuid) {
        this.similarFileUuid = similarFileUuid;
    }

    public String getSimilarFileName() {
        return similarFileName;
    }

    public void setSimilarFileName(String similarFileName) {
        this.similarFileName = similarFileName;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}