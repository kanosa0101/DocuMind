package com.javaee.aiservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI任务实体类
 */
@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskType;

    private String inputData;

    private String outputData;

    private Long docId;

    private Long userId;

    private String status;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime completeTime;
}