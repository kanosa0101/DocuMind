package com.javaee.fileservice.state;

/**
 * 处理状态枚举 (v3.0)
 * 定义文件处理过程中的各个状态
 */
public enum ProcessState {

    /** 待处理，文件刚上传 */
    PENDING("待处理"),

    /** 正在解析内容 */
    PARSE("正在解析"),

    /** 正在AI处理 */
    AI_PROCESSING("正在AI处理"),

    /** 正在智能分类 */
    CLASSIFYING("正在分类"),

    /** 正在索引知识库 */
    INDEXING("正在索引"),

    /** 处理完成 */
    COMPLETED("已完成"),

    /** 处理失败 */
    FAILED("处理失败"),

    /** 内容已更新，需要重新处理 */
    NEED_UPDATE("需要更新");

    private final String description;

    ProcessState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}