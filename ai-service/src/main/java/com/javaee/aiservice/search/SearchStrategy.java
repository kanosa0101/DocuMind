package com.javaee.aiservice.search;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 搜索策略枚举
 */
public enum SearchStrategy {
    /** RAG问答模式 */
    QA,
    /** 关键词搜索模式 */
    KEYWORD,
    /** 分类过滤模式 */
    CLASSIFY,
    /** 智能混合模式 */
    HYBRID
}