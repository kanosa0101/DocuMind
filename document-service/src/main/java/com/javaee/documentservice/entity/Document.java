package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体类
 * 注意：文档内容存储在MinIO，MySQL只存储元数据
 */
@Data
@TableName("doc_info")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /**
     * 文档内容存储在MinIO，此字段不映射到数据库
     * 仅用于临时存储从MinIO获取的内容
     */
    @TableField(exist = false)
    private String content;

    private String summary;

    private String keywords;

    private String category;

    private String tags;

    private Long fileId;

    private Long userId;

    private Integer version;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
