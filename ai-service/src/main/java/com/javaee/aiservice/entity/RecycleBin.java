package com.javaee.aiservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站实体类
 * 用于存储已删除文件的记录，支持在有效期内恢复
 */
@Data
@TableName("recycle_bin")
public class RecycleBin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回收站记录唯一标识(UUID)
     */
    private String recycleId;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * MinIO存储桶名称
     */
    private String bucketName;

    /**
     * 原始对象名称(MinIO中的路径)
     */
    private String objectName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型(MIME)
     */
    private String fileType;

    /**
     * 删除用户ID
     */
    private Long userId;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;

    /**
     * 过期时间(超过后自动永久删除)
     */
    private LocalDateTime expiryTime;

    /**
     * 状态: DELETED, RESTORED, PERMANENT_DELETED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    }