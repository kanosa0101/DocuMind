package com.javaee.aiservice.controller;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.dto.RagQueryDTO;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import com.javaee.aiservice.rag.VectorStore;
import com.javaee.aiservice.search.SearchStrategy;
import com.javaee.aiservice.search.SearchStrategyRouter;
import com.javaee.aiservice.service.AIService;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Autowired
    private SearchStrategyRouter strategyRouter;

    @Autowired
    private AIService aiService;

    /**
     * 文档索引（需认证）
     */
    @PostMapping("/index")
    @Operation(summary = "文档索引", description = "将文档添加到知识库，需用户认证")
    public Result<Void> indexDocument(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "文档标题") @RequestParam(required = false) String title,
            @Parameter(description = "文档内容") @RequestBody String content,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 验证用户身份
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return Result.fail("用户身份验证失败：缺少用户ID");
        }
        // 存储文档标题（用于前端显示）
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("userId", userIdHeader);
        if (title != null && !title.isEmpty()) {
            metadata.put("title", title);
        }
        knowledgeBase.addDocument(documentId, content, metadata);
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

        // 空字符串使用默认策略
        String effectiveStrategy = (strategy != null && !strategy.isEmpty()) ? strategy : "HYBRID";

        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(effectiveStrategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.fail("无效的重排序策略: " + effectiveStrategy);
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
     * 支持策略路由：自动判断输入是问句还是关键词
     */
    @PostMapping("/query")
    @Operation(summary = "知识库问答", description = "基于知识库进行问答，自动判断搜索策略")
    public Result<Map<String, Object>> query(@RequestBody RagQueryDTO dto) {
        String question = dto.getQuestion();
        // 空字符串也使用默认策略
        String strategy = (dto.getStrategy() != null && !dto.getStrategy().isEmpty())
            ? dto.getStrategy() : "HYBRID";

        if (question == null || question.trim().isEmpty()) {
            return Result.fail("问题内容不能为空");
        }

        // 使用策略路由检测输入类型
        SearchStrategy detectedStrategy = strategyRouter.detectStrategy(question);
        log.info("知识库问答: question={}, detectedStrategy={}, explicitStrategy={}",
                 question, detectedStrategy, strategy);

        try {
            // 根据策略选择不同的处理方式
            if (detectedStrategy == SearchStrategy.QA) {
                // QA模式：返回答案和来源
                return performQASearch(question, strategy);
            } else {
                // KEYWORD模式：返回文档列表
                return performKeywordSearch(question);
            }
        } catch (Exception e) {
            log.error("知识库问答失败: {}", e.getMessage(), e);
            return Result.fail("知识库问答失败: " + e.getMessage());
        }
    }

    /**
     * RAG问答搜索（返回答案和来源）
     */
    private Result<Map<String, Object>> performQASearch(String question, String strategy) {
        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的重排序策略: {}", strategy);
            rerankStrategy = Reranker.RerankStrategy.HYBRID;
        }

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

        // 构建带标题的sources列表
        List<Map<String, Object>> sources = results.stream()
            .map(r -> {
                Map<String, Object> source = new HashMap<>();
                source.put("id", r.get("id"));
                source.put("title", r.get("title") != null ? r.get("title") : "文档 " + r.get("id"));
                return source;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("sources", sources);
        response.put("strategy", "QA");
        response.put("retrievalStrategy", strategy);

        // 生成推荐问题
        try {
            List<String> recommendedQuestions = aiService.generateRecommendedQuestions(question, answer);
            response.put("recommendedQuestions", recommendedQuestions);
        } catch (Exception e) {
            log.warn("生成推荐问题失败: {}", e.getMessage());
            // 不影响主流程，继续返回
        }

        log.info("RAG问答完成: question={}, sourcesCount={}", question, results.size());
        return Result.success(response);
    }

    /**
     * 关键词搜索（返回文档列表）
     */
    private Result<Map<String, Object>> performKeywordSearch(String keyword) {
        List<Map<String, Object>> documents = knowledgeBase.hybridSearch(keyword, 10);

        // 构建文档列表
        List<Map<String, Object>> docList = documents.stream()
            .map(d -> {
                Map<String, Object> doc = new HashMap<>();
                doc.put("id", d.get("id"));
                doc.put("title", d.get("title") != null ? d.get("title") : "文档 " + d.get("id"));
                doc.put("similarity", d.get("similarity"));
                return doc;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("query", keyword);
        response.put("documents", docList);
        response.put("strategy", "KEYWORD");

        log.info("关键词搜索完成: keyword={}, documentsCount={}", keyword, documents.size());
        return Result.success(response);
    }

    /**
     * 知识库问答（流式输出）
     * SSE流式返回AI回复
     */
    @GetMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "知识库问答(流式)", description = "基于知识库进行问答，流式返回结果")
    public SseEmitter streamQuery(
            @Parameter(description = "问题") @RequestParam String question,
            @Parameter(description = "重排序策略") @RequestParam(defaultValue = "HYBRID") String strategy) {

        if (question == null || question.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send("[ERROR] 问题内容不能为空");
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        log.info("流式知识库问答: question={}, strategy={}", question, strategy);

        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                Reranker.RerankStrategy rerankStrategy;
                try {
                    rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
                } catch (IllegalArgumentException e) {
                    rerankStrategy = Reranker.RerankStrategy.HYBRID;
                }

                // 搜索相关文档
                List<Map<String, Object>> results = knowledgeBase.hybridSearchWithRerank(question, 3, rerankStrategy);

                StringBuilder context = new StringBuilder();
                for (Map<String, Object> result : results) {
                    Object content = result.get("content");
                    if (content != null) {
                        context.append(content.toString()).append("\n\n");
                    }
                }

                if (context.length() == 0) {
                    emitter.send("[ERROR] 知识库中没有找到相关内容");
                    emitter.complete();
                    return;
                }

                String prompt = promptEngineeringService.createQAPrompt(question, context.toString(), null);

                // 使用回调方式流式调用
                chatService.streamChatApi(
                    prompt,
                    chunk -> {
                        try {
                            emitter.send(chunk);
                        } catch (IOException e) {
                            log.error("发送SSE chunk失败", e);
                        }
                    },
                    () -> {
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("完成SSE失败", e);
                        }
                    },
                    error -> {
                        try {
                            emitter.send("[ERROR] " + error);
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                );

            } catch (Exception e) {
                log.error("流式知识库问答失败: {}", e.getMessage(), e);
                try {
                    emitter.send("[ERROR] " + e.getMessage());
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    emitter.completeWithError(ioException);
                }
            }
        });

        emitter.onCompletion(() -> executor.shutdown());
        emitter.onTimeout(() -> {
            log.warn("SSE超时");
            executor.shutdown();
        });
        emitter.onError(e -> {
            log.error("SSE错误", e);
            executor.shutdown();
        });

        return emitter;
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
     * 删除文档（物理删除，需认证）
     */
    @DeleteMapping("/document/{documentId}")
    @Operation(summary = "删除文档", description = "从知识库物理删除文档，需用户认证")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 验证用户身份
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return Result.fail("用户身份验证失败：缺少用户ID");
        }
        knowledgeBase.removeDocument(documentId);
        return Result.success();
    }

    /**
     * 软删除文档 (v3.0新增)
     * 标记文档deleted=true，搜索时过滤，可恢复
     */
    @PutMapping("/document/{documentId}/soft-delete")
    @Operation(summary = "软删除文档", description = "标记文档为已删除状态，搜索时过滤，可恢复")
    public Result<Void> softDeleteDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 验证用户身份
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return Result.fail("用户身份验证失败：缺少用户ID");
        }
        knowledgeBase.softDeleteDocument(documentId);
        return Result.success();
    }

    /**
     * 恢复文档 (v3.0新增)
     * 标记文档deleted=false，恢复可检索状态
     */
    @PutMapping("/document/{documentId}/restore")
    @Operation(summary = "恢复文档", description = "恢复软删除的文档，使其可检索")
    public Result<Void> restoreDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 验证用户身份
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return Result.fail("用户身份验证失败：缺少用户ID");
        }
        knowledgeBase.restoreDocument(documentId);
        return Result.success();
    }

    /**
     * 获取所有文档ID
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表", description = "获取知识库中的所有文档信息")
    public Result<List<Map<String, Object>>> getAllDocuments() {
        List<Map<String, Object>> documents = knowledgeBase.getAllDocuments();
        return Result.success(documents);
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
