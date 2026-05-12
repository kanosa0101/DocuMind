package com.javaee.documentservice.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本VO
 * 字段名与前端接口定义保持一致
 */
@Data
public class DocumentVersionVO {

    private String id;

    /**
     * 文档ID（对应前端的documentId）
     */
    private String documentId;

    /**
     * 版本号（对应前端的versionNumber）
     */
    private Integer versionNumber;

    private String content;

    /**
     * 变更日志（对应前端的changeLog）
     */
    private String changeLog;

    /**
     * 创建者（对应前端的createdBy）
     */
    private String createdBy;

    private LocalDateTime createTime;
}
