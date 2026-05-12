package com.javaee.documentservice.dto;

import lombok.Data;

import java.util.List;

/**
 * 文档更新DTO
 */
@Data
public class DocumentUpdateDTO {

    private String id;

    private String title;

    private String content;

    private String summary;

    private String category;

    private List<String> tags;

    private List<String> keywords;

    private String changeLog;
}
