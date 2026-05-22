package com.javaee.aiservice.service;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.dto.KeywordExtractDTO;
import com.javaee.aiservice.dto.TextAnalyzeDTO;
import com.javaee.aiservice.dto.TextSummarizeDTO;
import com.javaee.aiservice.vo.KeywordExtractVO;
import com.javaee.aiservice.vo.TextAnalyzeVO;
import com.javaee.aiservice.vo.TextSummarizeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AIService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private PromptEngineeringService promptEngineeringService;

    @InjectMocks
    private AIService aiService;

    private TextSummarizeDTO summarizeDTO;
    private TextAnalyzeDTO analyzeDTO;
    private KeywordExtractDTO keywordDTO;

    @BeforeEach
    void setUp() {
        summarizeDTO = new TextSummarizeDTO();
        summarizeDTO.setContent("这是一段测试文本，用于测试摘要功能。包含多个句子和不同类型的字符。");
        summarizeDTO.setMaxLength(100);

        analyzeDTO = new TextAnalyzeDTO();
        analyzeDTO.setContent("Hello World! 你好世界。12345");

        keywordDTO = new KeywordExtractDTO();
        keywordDTO.setContent("Java是一种面向对象的编程语言，广泛应用于企业开发。");
        keywordDTO.setCount(5);
    }

    @Test
    @DisplayName("文档分析测试 - 基本统计")
    void testAnalyzeBasicStats() {
        TextAnalyzeVO result = aiService.analyze(analyzeDTO);

        assertNotNull(result);
        // "Hello World! 你好世界。12345" 实际长度为23字符
        assertTrue(result.getTotalCharacters() > 0);
        assertTrue(result.getChineseCharacters() > 0); // 包含中文字符
        assertTrue(result.getEnglishCharacters() > 0); // 包含英文字符
        assertTrue(result.getDigits() > 0); // 包含数字
        assertTrue(result.getSentences() > 0); // 有句子
        assertTrue(result.getWords() > 0); // 有单词
    }

    @Test
    @DisplayName("文档分析测试 - 空文本")
    void testAnalyzeEmptyContent() {
        TextAnalyzeDTO dto = new TextAnalyzeDTO();
        dto.setContent("");

        TextAnalyzeVO result = aiService.analyze(dto);

        assertNotNull(result);
        assertEquals(0, result.getTotalCharacters());
    }

    @Test
    @DisplayName("文档分析测试 - 纯中文文本")
    void testAnalyzeChineseOnly() {
        TextAnalyzeDTO dto = new TextAnalyzeDTO();
        dto.setContent("这是纯中文文本测试");

        TextAnalyzeVO result = aiService.analyze(dto);

        assertNotNull(result);
        assertTrue(result.getChineseCharacters() > 0);
        assertEquals(0, result.getEnglishCharacters());
    }

    @Test
    @DisplayName("文档分析测试 - 纯英文文本")
    void testAnalyzeEnglishOnly() {
        TextAnalyzeDTO dto = new TextAnalyzeDTO();
        dto.setContent("This is pure English text test");

        TextAnalyzeVO result = aiService.analyze(dto);

        assertNotNull(result);
        assertEquals(0, result.getChineseCharacters());
        assertTrue(result.getEnglishCharacters() > 0);
    }

    @Test
    @DisplayName("文档摘要测试 - 正常调用")
    void testSummarizeNormal() {
        when(promptEngineeringService.createSummarizePrompt(anyString(), anyInt()))
                .thenReturn("summarize prompt");
        when(chatService.callChatApi("summarize prompt"))
                .thenReturn("这是摘要结果");

        TextSummarizeVO result = aiService.summarize(summarizeDTO);

        assertNotNull(result);
        assertEquals("这是摘要结果", result.getSummary());
        assertTrue(result.getOriginalLength() > 0);
        assertTrue(result.getCompressionRatio() > 0);

        verify(chatService).callChatApi(anyString());
    }

    @Test
    @DisplayName("文档摘要测试 - 空文本应抛出异常")
    void testSummarizeEmptyContent() {
        TextSummarizeDTO dto = new TextSummarizeDTO();
        dto.setContent("");

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.summarize(dto);
        });

        verify(chatService, never()).callChatApi(anyString());
    }

    @Test
    @DisplayName("文档摘要测试 - null文本应抛出异常")
    void testSummarizeNullContent() {
        TextSummarizeDTO dto = new TextSummarizeDTO();
        dto.setContent(null);

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.summarize(dto);
        });
    }

    @Test
    @DisplayName("关键词提取测试 - 正常调用")
    void testExtractKeywordsNormal() {
        when(promptEngineeringService.createKeywordExtractPrompt(anyString(), anyInt()))
                .thenReturn("keyword prompt");
        when(chatService.callChatApi("keyword prompt"))
                .thenReturn("Java, 编程, 语言, 企业, 开发");

        KeywordExtractVO result = aiService.extractKeywords(keywordDTO);

        assertNotNull(result);
        assertTrue(result.getTotalCount() > 0);
        assertTrue(result.getKeywords().size() > 0);
        assertNotNull(result.getKeywords().get(0).getWord());

        verify(chatService).callChatApi(anyString());
    }

    @Test
    @DisplayName("关键词提取测试 - 空文本应抛出异常")
    void testExtractKeywordsEmptyContent() {
        KeywordExtractDTO dto = new KeywordExtractDTO();
        dto.setContent("");

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.extractKeywords(dto);
        });

        verify(chatService, never()).callChatApi(anyString());
    }

    @Test
    @DisplayName("推荐问题生成测试")
    void testGenerateRecommendedQuestions() {
        when(chatService.callChatApi(anyString()))
                .thenReturn("问题1：Java有什么特点？\n问题2：如何学习Java？\n问题3：Java应用场景有哪些？");

        var result = aiService.generateRecommendedQuestions("什么是Java", "Java是一种编程语言");

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.size() <= 3);

        verify(chatService).callChatApi(anyString());
    }

    @Test
    @DisplayName("文档纠错测试")
    void testCorrect() {
        TextAnalyzeVO result = aiService.correct(analyzeDTO);

        assertNotNull(result);
        assertEquals(analyzeDTO.getContent().length(), result.getTotalCharacters());
    }
}