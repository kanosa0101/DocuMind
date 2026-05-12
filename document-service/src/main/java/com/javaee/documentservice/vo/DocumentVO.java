package com.javaee.documentservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档VO
 * 字段名与前端接口定义保持一致
 */
@Data
public class DocumentVO {

    private String id;

    private String title;

    private String content;

    private String summary;

    private List<String> keywords;

    private String fileId;

    private String category;

    private List<String> tags;

    private Integer version;

    private String status;

    private String userId;

    /**
     * 创建者（对应前端的createdBy字段）
     */
    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
