package com.javaee.aiservice.controller;

import com.javaee.aiservice.search.GlobalSearchService;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全局搜索控制器
 * 提供Header搜索框的全局搜索API
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/search")
@Tag(name = "全局搜索", description = "跨文档内容搜索和智能问答")
public class GlobalSearchController {

    @Autowired
    private GlobalSearchService globalSearchService;

    /**
     * 全局搜索
     * 智能判断搜索策略（问答/关键词/分类）
     */
    @GetMapping
    @Operation(summary = "全局搜索", description = "智能判断搜索模式：问答返回答案+来源，关键词返回文档列表")
    public Result<Map<String, Object>> search(
            @Parameter(description = "搜索词或问题") @RequestParam String query,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        if (query == null || query.trim().isEmpty()) {
            return Result.fail("搜索内容不能为空");
        }

        // 从Header获取用户ID
        Long effectiveUserId = userId;
        if (effectiveUserId == null && userIdHeader != null) {
            effectiveUserId = Long.parseLong(userIdHeader);
        }
        if (effectiveUserId == null) {
            effectiveUserId = 1L; // 默认用户
        }

        try {
            Map<String, Object> result = globalSearchService.search(query.trim(), effectiveUserId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("全局搜索失败: {}", e.getMessage(), e);
            return Result.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索策略检测
     */
    @GetMapping("/strategy")
    @Operation(summary = "检测搜索策略", description = "检测输入应该使用哪种搜索策略")
    public Result<String> detectStrategy(
            @Parameter(description = "搜索词或问题") @RequestParam String query) {

        if (query == null || query.trim().isEmpty()) {
            return Result.fail("内容不能为空");
        }

        // 这里可以返回策略检测结果，让前端知道将使用哪种模式
        Object strategy = globalSearchService.search(query.trim(), 1L).get("strategy");
        return Result.success(strategy != null ? strategy.toString() : "KEYWORD");
    }
}