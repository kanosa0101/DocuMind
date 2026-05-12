package com.javaee.aiservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent对话请求DTO
 */
@Data
@Schema(description = "Agent对话消息请求")
public class AgentChatDTO {

    @Schema(description = "用户输入内容", required = true)
    private String userInput;
}