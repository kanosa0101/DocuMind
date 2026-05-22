package com.javaee.fileservice.client;

import com.javaee.common.model.Result;
import com.javaee.fileservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * AI服务Feign客户端 (v3.0)
 */
@FeignClient(name = "ai-service", configuration = FeignConfig.class)
public interface AIServiceClient {

    /**
     * 文本摘要
     */
    @PostMapping("/api/ai/summarize")
    Result<Map<String, Object>> summarize(@RequestBody Map<String, Object> request);

    /**
     * 关键词提取
     */
    @PostMapping("/api/ai/keywords")
    Result<Map<String, Object>> extractKeywords(@RequestBody Map<String, Object> request);

    /**
     * 文本分类
     */
    @PostMapping("/api/ai/classify")
    Result<Map<String, Object>> classify(@RequestBody Map<String, Object> request);

    /**
     * RAG索引文档
     * 参数名与RagController保持一致：documentId, title
     * 需要传递X-User-Id请求头用于用户隔离
     */
    @PostMapping("/api/ai/rag/index")
    Result<Void> indexDocument(@RequestParam("documentId") String documentId,
                               @RequestParam(value = "title", required = false) String title,
                               @RequestBody String content,
                               @RequestHeader("X-User-Id") String userId);

    /**
     * RAG删除文档索引（物理删除）
     * 使用DELETE方法，路径与RagController一致
     */
    @DeleteMapping("/api/ai/rag/document/{documentId}")
    Result<Void> deleteDocument(@PathVariable("documentId") String documentId,
                                @RequestHeader("X-User-Id") String userId);

    /**
     * RAG软删除文档索引 (v3.0新增)
     * 标记向量deleted=true，搜索时过滤，可恢复
     */
    @PutMapping("/api/ai/rag/document/{documentId}/soft-delete")
    Result<Void> softDeleteVector(@PathVariable("documentId") String documentId,
                                  @RequestHeader("X-User-Id") String userId);

    /**
     * RAG恢复文档索引 (v3.0新增)
     * 标记向量deleted=false，恢复可检索状态
     */
    @PutMapping("/api/ai/rag/document/{documentId}/restore")
    Result<Void> restoreVector(@PathVariable("documentId") String documentId,
                               @RequestHeader("X-User-Id") String userId);

    /**
     * RAG相似内容搜索 (v3.0新增)
     * 用于相似度检测，返回相似文档列表
     */
    @GetMapping("/api/ai/rag/search")
    Result<List<Map<String, Object>>> searchSimilar(@RequestParam("query") String query,
                                                    @RequestParam("topK") int topK);
}