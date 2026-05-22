package com.javaee.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体类 (v3.0)
 * 合并 file_metadata + doc_info + doc_version 为单一实体
 * 核心设计: 文件即一切
 */
@Data
@TableName("file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileUuid;
    private String fileName;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String storagePath;

    private String summary;
    private String keywords;
    private String category;

    private Integer version;
    private String versionHistory;

    private Boolean indexed;
    private String vectorId;
    private LocalDateTime indexTime;

    private String processStatus;
    private LocalDateTime processTime;
    private Integer retryCount;

    private Long userId;
    private String status;
    private LocalDateTime deleteTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}