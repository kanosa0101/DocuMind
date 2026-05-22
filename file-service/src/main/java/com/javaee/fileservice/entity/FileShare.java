package com.javaee.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 文件分享实体类 (v3.0)
 * 内部分享：用户之间分享文件
 */
@TableName("file_share")
public class FileShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分享的文件UUID
     */
    private String fileUuid;

    /**
     * 文件所有者ID
     */
    private Long ownerId;

    /**
     * 分享给的用户ID
     */
    private Long shareToId;

    /**
     * 权限类型
     * VIEW: 只查看
     * VIEW_DOWNLOAD: 查看且下载
     */
    private String permission;

    /**
     * 过期时间（NULL=永久）
     */
    private LocalDateTime expireTime;

    /**
     * 状态
     * ACTIVE: 有效
     * EXPIRED: 已过期
     * CANCELLED: 已取消
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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileUuid() {
        return fileUuid;
    }

    public void setFileUuid(String fileUuid) {
        this.fileUuid = fileUuid;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getShareToId() {
        return shareToId;
    }

    public void setShareToId(Long shareToId) {
        this.shareToId = shareToId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}