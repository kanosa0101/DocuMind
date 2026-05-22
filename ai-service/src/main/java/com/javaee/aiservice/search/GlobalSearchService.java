package com.javaee.aiservice.search;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 * 根据策略路由选择合适的搜索方式
 */
@Service
public class GlobalSearchService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSearchService.class);

    @Autowired
    private SearchStrategyRouter strategyRouter;

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private ChatService chatService;

    @Autowired
    private PromptEngineeringService promptEngineeringService;

    /**
     * 全局搜索
     * @param query 搜索词/问题
     * @param userId 用户ID
     * @return 搜索结果
     */
    public Map<String, Object> search(String query, Long userId) {
        SearchStrategy strategy = strategyRouter.detectStrategy(query);
        log.info("全局搜索: query={}, strategy={}, userId={}", query, strategy, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("strategy", strategy.name());

        switch (strategy) {
            case QA:
                // RAG问答模式
                Map<String, Object> qaResult = performQASearch(query);
                result.put("answer", qaResult.get("answer"));
                result.put("sources", qaResult.get("sources"));
                break;

            case CLASSIFY:
                // 分类过滤模式（暂不实现，返回提示）
                result.put("message", "分类搜索功能即将上线");
                result.put("documents", List.of());
                break;

            case KEYWORD:
                // 关键词搜索模式
                List<Map<String, Object>> documents = performKeywordSearch(query);
                result.put("documents", documents);
                break;

            default:
                result.put("documents", performKeywordSearch(query));
        }

        log.info("搜索完成: strategy={}, results={}", strategy, result.size());
        return result;
    }

    /**
     * RAG问答搜索
     */
    private Map<String, Object> performQASearch(String question) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 使用混合检索加重排序
            List<Map<String, Object>> searchResults = knowledgeBase.hybridSearchWithRerank(
                    question, 3, Reranker.RerankStrategy.HYBRID);

            if (searchResults.isEmpty()) {
                result.put("answer", "知识库中没有找到相关内容，请先上传相关文档。");
                result.put("sources", List.of());
                return result;
            }

            // 构建上下文
            StringBuilder context = new StringBuilder();
            for (Map<String, Object> doc : searchResults) {
                Object content = doc.get("content");
                if (content != null) {
                    context.append(content.toString()).append("\n\n");
                }
            }

            // 调用AI生成答案
            String prompt = promptEngineeringService.createQAPrompt(question, context.toString(), null);
            String answer = chatService.callChatApi(prompt);

            result.put("answer", answer);

            // 构建来源列表
            List<Map<String, Object>> sources = searchResults.stream()
                    .map(doc -> {
                        Map<String, Object> source = new HashMap<>();
                        source.put("id", doc.get("id"));
                        source.put("title", doc.get("title") != null ? doc.get("title") : "文档 " + doc.get("id"));
                        source.put("similarity", doc.get("similarity"));
                        return source;
                    })
                    .collect(Collectors.toList());

            result.put("sources", sources);

        } catch (Exception e) {
            log.error("RAG问答搜索失败: {}", e.getMessage(), e);
            result.put("answer", "搜索过程中出现错误，请稍后重试。");
            result.put("sources", List.of());
        }

        return result;
    }

    /**
     * 关键词搜索
     */
    private List<Map<String, Object>> performKeywordSearch(String keyword) {
        try {
            return knowledgeBase.hybridSearch(keyword, 10);
        } catch (Exception e) {
            log.error("关键词搜索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }
}