package com.javaee.aiservice.controller;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.dto.RagQueryDTO;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import com.javaee.aiservice.rag.VectorStore;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG控制器
 * 提供知识库相关的REST API接口
 * 支持基础检索、混合检索和重排序功能
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/rag")
@Tag(name = "RAG知识库", description = "知识库索引、搜索、问答接口")
public class RagController {

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private Reranker reranker;

    @Autowired
    private ChatService chatService;

    @Autowired
    private PromptEngineeringService promptEngineeringService;

    /**
     * 文档索引
     */
    @PostMapping("/index")
    @Operation(summary = "文档索引", description = "将文档添加到知识库")
    public Result<Void> indexDocument(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "文档内容") @RequestBody String content) {
        knowledgeBase.addDocument(documentId, content, Map.of());
        return Result.success();
    }

    /**
     * 基础向量检索
     */
    @GetMapping("/search")
    @Operation(summary = "基础检索", description = "使用向量相似度搜索知识库")
    public Result<List<Map<String, Object>>> search(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK) {
        List<Map<String, Object>> results = knowledgeBase.search(query, topK);
        return Result.success(results);
    }

    /**
     * 混合检索（向量检索 + BM25）
     */
    @GetMapping("/search/hybrid")
    @Operation(summary = "混合检索", description = "使用向量检索和BM25检索的混合方式搜索")
    public Result<List<Map<String, Object>>> hybridSearch(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK) {
        List<Map<String, Object>> results = knowledgeBase.hybridSearch(query, topK);
        return Result.success(results);
    }

    /**
     * 混合检索加重排序
     */
    @GetMapping("/search/hybrid/rerank")
    @Operation(summary = "混合检索加重排序", description = "混合检索后使用指定策略进行重排序")
    public Result<List<Map<String, Object>>> hybridSearchWithRerank(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "重排序策略: BM25_FUSION, CROSS_ENCODER, HYBRID") 
            @RequestParam(defaultValue = "HYBRID") String strategy) {
        
        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.fail("无效的重排序策略: " + strategy);
        }
        
        List<Map<String, Object>> results = knowledgeBase.hybridSearchWithRerank(query, topK, rerankStrategy);
        return Result.success(results);
    }

    /**
     * 获取支持的重排序策略
     */
    @GetMapping("/rerank/strategies")
    @Operation(summary = "获取重排序策略", description = "获取所有支持的重排序策略")
    public Result<List<String>> getRerankStrategies() {
        List<String> strategies = reranker.getSupportedStrategies();
        return Result.success(strategies);
    }

    /**
     * 知识库问答（使用混合检索加重排序）
     */
    @PostMapping("/query")
    @Operation(summary = "知识库问答", description = "基于知识库进行问答，默认使用混合检索加重排序")
    public Result<Map<String, Object>> query(@RequestBody RagQueryDTO dto) {
        String question = dto.getQuestion();
        String strategy = dto.getStrategy() != null ? dto.getStrategy() : "HYBRID";

        if (question == null || question.trim().isEmpty()) {
            return Result.fail("问题内容不能为空");
        }

        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的重排序策略: {}", strategy);
            return Result.fail("无效的重排序策略: " + strategy);
        }

        try {
            // 使用混合检索加重排序获取相关文档
            List<Map<String, Object>> results = knowledgeBase.hybridSearchWithRerank(question, 3, rerankStrategy);

            StringBuilder context = new StringBuilder();
            for (Map<String, Object> result : results) {
                Object content = result.get("content");
                if (content != null) {
                    context.append(content.toString()).append("\n\n");
                }
            }

            // 调用AI模型生成答案
            String answer;
            if (context.length() > 0) {
                String prompt = promptEngineeringService.createQAPrompt(question, context.toString(), null);
                answer = chatService.callChatApi(prompt);
            } else {
                answer = "知识库中没有找到相关内容，无法生成答案。请先索引相关文档。";
            }

            Map<String, Object> response = Map.of(
                "question", question,
                "context", context.toString(),
                "answer", answer,
                "sources", results.stream().map(r -> r.get("id")).toList(),
                "retrievalStrategy", strategy
            );

            log.info("知识库问答完成: question={}, sourcesCount={}", question, results.size());
            return Result.success(response);
        } catch (Exception e) {
            log.error("知识库问答失败: {}", e.getMessage(), e);
            return Result.fail("知识库问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/document/{documentId}")
    @Operation(summary = "获取文档内容", description = "获取知识库中的文档内容")
    public Result<String> getDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        String content = knowledgeBase.getDocumentContent(documentId);
        return Result.success(content);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/document/{documentId}")
    @Operation(summary = "删除文档", description = "从知识库删除文档")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        knowledgeBase.removeDocument(documentId);
        return Result.success();
    }

    /**
     * 获取所有文档ID
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表", description = "获取知识库中的所有文档ID")
    public Result<List<String>> getAllDocuments() {
        List<String> documentIds = knowledgeBase.getAllDocumentIds();
        return Result.success(documentIds);
    }

    /**
     * 获取文档元数据
     */
    @GetMapping("/document/{documentId}/metadata")
    @Operation(summary = "获取文档元数据", description = "获取文档的元数据信息")
    public Result<Map<String, Object>> getDocumentMetadata(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        Map<String, Object> metadata = knowledgeBase.getDocumentMetadata(documentId);
        return Result.success(metadata);
    }
}
