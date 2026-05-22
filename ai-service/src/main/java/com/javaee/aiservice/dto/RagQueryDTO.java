package com.javaee.aiservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * RAG查询请求DTO
 */
@Data
@Schema(description = "RAG知识库查询请求")
public class RagQueryDTO {

    @Schema(description = "问题内容", required = true)
    private String question;

    @Schema(description = "重排序策略: BM25_FUSION, CROSS_ENCODER, HYBRID", defaultValue = "HYBRID")
    private String strategy;

    }