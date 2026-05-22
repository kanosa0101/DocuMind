package com.javaee.aiservice.service;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.dto.KeywordExtractDTO;
import com.javaee.aiservice.dto.TextAnalyzeDTO;
import com.javaee.aiservice.dto.TextSummarizeDTO;
import com.javaee.aiservice.vo.KeywordExtractVO;
import com.javaee.aiservice.vo.KeywordVO;
import com.javaee.aiservice.vo.TextAnalyzeVO;
import com.javaee.aiservice.vo.TextSummarizeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI服务
 * 提供文档摘要、纠错、关键词提取等功能
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private PromptEngineeringService promptEngineeringService;

    /**
     * 文档摘要
     * @param dto 请求参数
     * @return 摘要结果
     */
    public TextSummarizeVO summarize(TextSummarizeDTO dto) {
        String content = dto.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }

        int maxLength = dto.getMaxLength() != null ? dto.getMaxLength() : 200;
        log.info("开始文档摘要，文本长度={}, 最大长度={}", content.length(), maxLength);

        String summary = chatService.callChatApi(
            promptEngineeringService.createSummarizePrompt(content, maxLength)
        );

        TextSummarizeVO vo = new TextSummarizeVO();
        vo.setSummary(summary);
        vo.setOriginalLength(content.length());
        vo.setSummaryLength(summary.length());
        vo.setCompressionRatio((double) summary.length() / content.length());

        log.info("文档摘要完成");
        return vo;
    }

    /**
     * 文档分析
     * @param dto 请求参数
     * @return 分析结果
     */
    public TextAnalyzeVO analyze(TextAnalyzeDTO dto) {
        log.info("开始文档分析");

        String content = dto.getContent();

        TextAnalyzeVO vo = new TextAnalyzeVO();
        vo.setTotalCharacters(content.length());

        int chineseCount = 0, englishCount = 0, digitCount = 0, spaceCount = 0, punctuationCount = 0;

        for (char c : content.toCharArray()) {
            if (Character.isIdeographic(c)) {
                chineseCount++;
            } else if (Character.isLetter(c)) {
                englishCount++;
            } else if (Character.isDigit(c)) {
                digitCount++;
            } else if (Character.isWhitespace(c)) {
                spaceCount++;
            } else if (isPunctuation(c)) {
                punctuationCount++;
            }
        }

        vo.setChineseCharacters(chineseCount);
        vo.setEnglishCharacters(englishCount);
        vo.setDigits(digitCount);
        vo.setSpaces(spaceCount);
        vo.setPunctuations(punctuationCount);

        // 计算行数
        String[] lines = content.split("\n");
        vo.setLines(lines.length);

        // 计算句子数（基于中文和英文句号）
        int sentenceCount = 0;
        for (String line : lines) {
            sentenceCount += countSentences(line);
        }
        vo.setSentences(sentenceCount);

        // 计算单词数（英文单词 + 中文词组估算）
        int wordCount = countWords(content);
        vo.setWords(wordCount);

        log.info("文档分析完成: totalChars={}, sentences={}, words={}",
            vo.getTotalCharacters(), vo.getSentences(), vo.getWords());
        return vo;
    }

    /**
     * 计算句子数
     */
    private int countSentences(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') {
                count++;
            }
        }
        // 至少返回1，如果文本不为空且没有句号
        return count > 0 ? count : (text.trim().isEmpty() ? 0 : 1);
    }

    /**
     * 计算单词数
     */
    private int countWords(String text) {
        // 英文单词计数 - 使用简单的空格和标点分隔
        String[] englishWords = text.split("[\\s\\p{Punct}]+");
        int englishWordCount = 0;
        for (String word : englishWords) {
            if (word.matches("[a-zA-Z]+") && word.length() > 0) {
                englishWordCount++;
            }
        }

        // 中文字符作为词估算（每个汉字算一个词）
        int chineseWordCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.isIdeographic(c)) {
                chineseWordCount++;
            }
        }

        return englishWordCount + chineseWordCount;
    }

    /**
     * 判断是否为标点符号
     */
    private boolean isPunctuation(char c) {
        return "，。！？；：、\"\"''（）{}[]<>《》·".indexOf(c) >= 0 ||
               ",.!?;:\"'(){}[]<>".indexOf(c) >= 0;
    }

    /**
     * 关键词提取
     * @param dto 请求参数
     * @return 关键词结果
     */
    public KeywordExtractVO extractKeywords(KeywordExtractDTO dto) {
        String content = dto.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }

        int count = dto.getCount() != null ? dto.getCount() : 10;
        log.info("开始关键词提取，文本长度={}, 关键词数量={}", content.length(), count);

        String keywordsStr = chatService.callChatApi(
            promptEngineeringService.createKeywordExtractPrompt(content, count)
        );

        // 解析AI返回的关键词，支持中英文逗号和换行分隔
        List<KeywordVO> keywords = Arrays.stream(keywordsStr.split("[,，\n]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(count)
            .map(word -> new KeywordVO(word, calculateKeywordScore(word, content), "keyword"))
            .collect(Collectors.toList());

        KeywordExtractVO vo = new KeywordExtractVO();
        vo.setKeywords(keywords);
        vo.setTotalCount(keywords.size());

        log.info("关键词提取完成，共{}个关键词", keywords.size());
        return vo;
    }

    /**
     * 计算关键词分数（基于词频）
     */
    private Double calculateKeywordScore(String keyword, String content) {
        if (keyword == null || keyword.isEmpty()) {
            return 0.0;
        }
        // 简单的词频计算
        int occurrences = 0;
        int index = 0;
        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        while ((index = lowerContent.indexOf(lowerKeyword, index)) != -1) {
            occurrences++;
            index += lowerKeyword.length();
        }
        // 归一化分数：词频/总词数
        int totalWords = content.length();
        return totalWords > 0 ? Math.min(1.0, (double) occurrences * keyword.length() / totalWords) : 0.5;
    }

    /**
     * 文档纠错
     * @param dto 请求参数
     * @return 分析结果
     */
    public TextAnalyzeVO correct(TextAnalyzeDTO dto) {
        log.info("开始文档纠错");
        return analyze(dto);
    }

    /**
     * 生成推荐问题
     * 基于当前问题和答案，生成用户可能感兴趣的后续问题
     * @param question 用户问题
     * @param answer AI回答
     * @return 推荐问题列表（最多3个）
     */
    public List<String> generateRecommendedQuestions(String question, String answer) {
        log.info("开始生成推荐问题");

        String prompt = String.format(
            "基于以下问答内容，生成3个用户可能感兴趣的后续问题。\n" +
            "要求：\n" +
            "1. 问题应与原话题相关，但有不同角度\n" +
            "2. 问题简洁明了，不要过长\n" +
            "3. 直接输出问题列表，每行一个问题，不要编号和额外说明\n\n" +
            "问题: %s\n答案: %s",
            question, answer
        );

        String response = chatService.callChatApi(prompt);

        // 解析返回的问题列表
        List<String> questions = Arrays.stream(response.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> s.length() > 5 && s.length() < 100) // 过滤过短或过长的行
            .limit(3)
            .collect(Collectors.toList());

        log.info("推荐问题生成完成，共{}个问题", questions.size());
        return questions;
    }
}