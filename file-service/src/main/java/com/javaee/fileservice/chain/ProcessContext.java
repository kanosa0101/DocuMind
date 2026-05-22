package com.javaee.fileservice.chain;

import com.javaee.fileservice.state.ProcessState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理上下文 (v3.0)
 * 存储整个处理链过程中的数据
 * 适配v3.0 FileInfo模型
 */
@Data
public class ProcessContext {

    /** 文件UUID */
    private String fileUuid;

    /** 文件名 */
    private String fileName;

    /** 用户ID */
    private Long userId;

    /** 存储路径 */
    private String storagePath;

    /** 解析后的文件内容 */
    private String content;

    /** AI生成的摘要 */
    private String summary;

    /** AI提取的关键词 */
    private List<String> keywords;

    /** 智能分类 */
    private String category;

    /** 是否已索引到知识库 */
    private boolean indexed;

    /** 向量ID */
    private String vectorId;

    /** 当前处理状态 */
    private ProcessState processStatus;

    /** 处理模式: NEW(新建) 或 UPDATE_VERSION(版本更新) */
    private String action = "NEW";

    /** 是否为版本更新 */
    private boolean versionUpdate = false;

    /** 当前版本号 (v3.0新增) */
    private Integer version = 1;

    /** 版本变更说明 */
    private String changeSummary;

    /** 当前执行步骤 */
    private String currentStep;

    /** 当前步骤索引 */
    private int currentStepIndex;

    /** 总步骤数 */
    private int totalSteps;

    /** 重试次数 */
    private int retryCount;

    /** 错误列表 */
    private List<String> errors = new ArrayList<>();

    /** 处理开始时间 */
    private Long startTime;

    /** 处理结束时间 */
    private Long endTime;

    /**
     * 计算当前进度百分比
     */
    public int getProgress() {
        if (totalSteps == 0) return 0;
        return (int) ((currentStepIndex + 1) * 100.0 / totalSteps);
    }

    /**
     * 添加错误信息
     */
    public void addError(String error) {
        errors.add(error);
    }

    /**
     * 获取处理耗时（毫秒）
     */
    public long getDuration() {
        if (startTime == null) return 0;
        if (endTime == null) return System.currentTimeMillis() - startTime;
        return endTime - startTime;
    }
}