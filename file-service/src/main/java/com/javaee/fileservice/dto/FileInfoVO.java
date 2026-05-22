package com.javaee.fileservice.dto;

import com.javaee.fileservice.entity.FileInfo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息展示VO (v3.0)
 * 用于前端展示，确保使用originalName作为显示名称
 */
@Data
public class FileInfoVO {

    private Long id;
    private String fileUuid;
    private String fileName;      // 显示名称（原始文件名）
    private String fileType;
    private Long fileSize;
    private String storagePath;

    private String summary;
    private String keywords;
    private String category;

    private Integer version;
    private String versionHistory;

    private Boolean indexed;
    private String processStatus;
    private LocalDateTime processTime;

    private Long userId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从FileInfo转换为VO
     */
    public static FileInfoVO fromFileInfo(FileInfo fileInfo) {
        FileInfoVO vo = new FileInfoVO();
        vo.setId(fileInfo.getId());
        vo.setFileUuid(fileInfo.getFileUuid());
        vo.setFileName(fileInfo.getOriginalName());  // 使用原始文件名作为显示名称
        vo.setFileType(fileInfo.getFileType());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setStoragePath(fileInfo.getStoragePath());
        vo.setSummary(fileInfo.getSummary());
        vo.setKeywords(fileInfo.getKeywords());
        vo.setCategory(fileInfo.getCategory());
        vo.setVersion(fileInfo.getVersion());
        vo.setVersionHistory(fileInfo.getVersionHistory());
        vo.setIndexed(fileInfo.getIndexed());
        vo.setProcessStatus(fileInfo.getProcessStatus());
        vo.setProcessTime(fileInfo.getProcessTime());
        vo.setUserId(fileInfo.getUserId());
        vo.setStatus(fileInfo.getStatus());
        vo.setCreateTime(fileInfo.getCreateTime());
        vo.setUpdateTime(fileInfo.getUpdateTime());
        return vo;
    }
}