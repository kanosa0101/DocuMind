package com.javaee.aiservice.search;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 搜索策略路由
 * 根据用户输入智能判断应该使用哪种搜索方式
 */
@Component
public class SearchStrategyRouter {

    private static final List<String> QUESTION_MARKERS = Arrays.asList(
            "如何", "怎么", "什么是", "为什么", "哪个", "哪些",
            "怎样", "怎样做", "能否", "可以", "吗", "呢", "?", "？"
    );

    private static final List<String> CATEGORIES = Arrays.asList(
            "技术文档", "分析报告", "会议记录", "项目文档", "其他"
    );

    /**
     * 检测搜索策略
     * @param query 用户输入
     * @return 搜索策略
     */
    public SearchStrategy detectStrategy(String query) {
        if (query == null || query.trim().isEmpty()) {
            return SearchStrategy.KEYWORD;
        }

        String trimmedQuery = query.trim();

        // 检查是否为问句
        if (isQuestion(trimmedQuery)) {
            return SearchStrategy.QA;
        }

        // 检查是否为分类搜索
        if (isCategory(trimmedQuery)) {
            return SearchStrategy.CLASSIFY;
        }

        // 默认关键词搜索
        return SearchStrategy.KEYWORD;
    }

    /**
     * 判断是否为问句
     */
    private boolean isQuestion(String text) {
        for (String marker : QUESTION_MARKERS) {
            if (text.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为分类搜索
     */
    private boolean isCategory(String text) {
        return CATEGORIES.contains(text);
    }
}