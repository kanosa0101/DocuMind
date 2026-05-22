package com.javaee.fileservice.dto;

/**
 * 文件相似度检测请求DTO
 * 使用POST请求避免中文编码问题
 */
public class SimilarityCheckDTO {

    /**
     * 新文件名
     */
    private String fileName;

    /**
     * 文件类型（可选）
     */
    private String fileType;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}