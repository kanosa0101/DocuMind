package com.javaee.aiservice.controller;

import com.javaee.aiservice.dto.*;
import com.javaee.aiservice.service.*;
import com.javaee.aiservice.vo.*;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI控制器
 * 提供AI处理相关的REST API接口
 *
 * 注意: 文件操作接口已移至 file-service 的 FileController
 * 本控制器仅保留纯AI处理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI处理", description = "文档摘要、关键词提取、文档分析等AI处理接口")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 文档摘要
     * @param dto 摘要请求参数
     * @return 摘要结果
     */
    @PostMapping("/summarize")
    @Operation(summary = "文档摘要", description = "对文档进行智能摘要")
    public Result<TextSummarizeVO> summarize(@RequestBody TextSummarizeDTO dto) {
        TextSummarizeVO vo = aiService.summarize(dto);
        return Result.success(vo);
    }

    /**
     * 关键词提取
     * @param dto 关键词提取请求参数
     * @return 关键词结果
     */
    @PostMapping("/keywords")
    @Operation(summary = "关键词提取", description = "从文档中提取关键词")
    public Result<KeywordExtractVO> extractKeywords(@RequestBody KeywordExtractDTO dto) {
        KeywordExtractVO vo = aiService.extractKeywords(dto);
        return Result.success(vo);
    }

    /**
     * 文档分析
     * @param dto 文档分析请求参数
     * @return 分析结果
     */
    @PostMapping("/analyze")
    @Operation(summary = "文档分析", description = "对文档进行深度分析")
    public Result<TextAnalyzeVO> analyze(@RequestBody TextAnalyzeDTO dto) {
        TextAnalyzeVO vo = aiService.analyze(dto);
        return Result.success(vo);
    }
}
