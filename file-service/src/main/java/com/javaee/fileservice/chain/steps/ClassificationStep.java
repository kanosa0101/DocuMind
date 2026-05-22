package com.javaee.fileservice.chain.steps;

import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.chain.ProcessResult;
import com.javaee.fileservice.chain.ProcessStep;
import com.javaee.fileservice.chain.RecoveryAction;
import com.javaee.fileservice.state.ProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 智能分类步骤 (v3.0)
 * 根据文档内容自动分类
 */
@Component
public class ClassificationStep implements ProcessStep {

    private static final Logger log = LoggerFactory.getLogger(ClassificationStep.class);

    private static final String DEFAULT_CATEGORY = "其他";
    private static final List<String> CATEGORIES = Arrays.asList(
            "技术文档", "分析报告", "会议记录", "项目文档", "论文", "试卷", "其他"
    );

    private static final List<String> TECH_KEYWORDS = Arrays.asList(
            "api", "接口", "架构", "系统", "代码", "数据库", "redis", "mysql",
            "java", "python", "vue", "react", "spring", "docker", "kubernetes",
            "技术", "开发", "实现", "部署", "测试", "调试", "bug", "功能"
    );

    private static final List<String> REPORT_KEYWORDS = Arrays.asList(
            "分析", "报告", "数据", "统计", "调研", "结论", "建议", "趋势",
            "市场", "竞品", "行业", "报告摘要", "调研报告", "分析报告"
    );

    private static final List<String> MEETING_KEYWORDS = Arrays.asList(
            "会议", "纪要", "讨论", "决议", "参会", "议题", "发言人", "时间",
            "会议室", "会议记录", "会议纪要", "议程", "与会", "出席"
    );

    private static final List<String> PROJECT_KEYWORDS = Arrays.asList(
            "项目", "需求", "规划", "进度", "里程碑", "版本", "迭代", "评审",
            "项目文档", "需求文档", "产品需求", "项目计划", "项目总结"
    );

    private static final List<String> PAPER_KEYWORDS = Arrays.asList(
            "论文", "研究", "学术", "毕业论文", "期刊", "摘要", "关键词",
            "参考文献", "引言", "综述", "结论", "作者", "硕士", "博士",
            "university", "research", "paper", "abstract", "conclusion"
    );

    private static final List<String> EXAM_KEYWORDS = Arrays.asList(
            "试卷", "考试", "试题", "答题", "题目", "答案", "分数", "满分",
            "选择题", "填空题", "简答题", "论述题", "期末考试", "期中考试",
            "测验", "考试题", "试卷答案", "考试大纲"
    );

    @Override
    public String getStepName() {
        return "CLASSIFICATION";
    }

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public ProcessResult execute(ProcessContext context) {
        log.info("开始智能分类: fileUuid={}", context.getFileUuid());

        String content = context.getContent();
        String title = context.getFileName();
        List<String> keywords = context.getKeywords();

        String category = classifyDocument(title, content, keywords);
        context.setCategory(category);
        context.setProcessStatus(ProcessState.CLASSIFYING);

        log.info("智能分类完成: fileUuid={}, category={}", context.getFileUuid(), category);
        return ProcessResult.success("分类成功: " + category);
    }

    /**
     * 根据标题、内容和关键词判断文档分类
     */
    private String classifyDocument(String title, String content, List<String> keywords) {
        // 标题权重更高，优先判断
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("论文") || lowerTitle.contains("paper")) {
                return "论文";
            }
            if (lowerTitle.contains("试卷") || lowerTitle.contains("考试") || lowerTitle.contains("试题")) {
                return "试卷";
            }
            if (lowerTitle.contains("api") || lowerTitle.contains("接口") || lowerTitle.contains("技术文档")) {
                return "技术文档";
            }
            if (lowerTitle.contains("会议") || lowerTitle.contains("纪要")) {
                return "会议记录";
            }
            if (lowerTitle.contains("项目") || lowerTitle.contains("需求") || lowerTitle.contains("规划")) {
                return "项目文档";
            }
        }

        // 内容分析
        String textToAnalyze = (title != null ? title : "") + " " +
                               (content != null ? content.substring(0, Math.min(content.length(), 500)) : "");
        textToAnalyze = textToAnalyze.toLowerCase();

        // 计算各分类的匹配分数
        int techScore = countKeywordMatches(textToAnalyze, TECH_KEYWORDS);
        int reportScore = countKeywordMatches(textToAnalyze, REPORT_KEYWORDS);
        int meetingScore = countKeywordMatches(textToAnalyze, MEETING_KEYWORDS);
        int projectScore = countKeywordMatches(textToAnalyze, PROJECT_KEYWORDS);
        int paperScore = countKeywordMatches(textToAnalyze, PAPER_KEYWORDS);
        int examScore = countKeywordMatches(textToAnalyze, EXAM_KEYWORDS);

        // AI关键词加分
        if (keywords != null) {
            for (String keyword : keywords) {
                String kw = keyword.toLowerCase();
                if (TECH_KEYWORDS.contains(kw)) techScore += 2;
                if (REPORT_KEYWORDS.contains(kw)) reportScore += 2;
                if (MEETING_KEYWORDS.contains(kw)) meetingScore += 2;
                if (PROJECT_KEYWORDS.contains(kw)) projectScore += 2;
                if (PAPER_KEYWORDS.contains(kw)) paperScore += 2;
                if (EXAM_KEYWORDS.contains(kw)) examScore += 2;
            }
        }

        // 选择分数最高的分类
        int maxScore = Math.max(Math.max(Math.max(techScore, reportScore), Math.max(meetingScore, projectScore)),
                                Math.max(paperScore, examScore));
        if (maxScore == 0) return DEFAULT_CATEGORY;

        if (paperScore >= maxScore) return "论文";
        if (examScore >= maxScore) return "试卷";
        if (techScore >= maxScore) return "技术文档";
        if (reportScore >= maxScore) return "分析报告";
        if (meetingScore >= maxScore) return "会议记录";
        if (projectScore >= maxScore) return "项目文档";

        return DEFAULT_CATEGORY;
    }

    /**
     * 计算关键词匹配数
     */
    private int countKeywordMatches(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean shouldSkip(ProcessContext context) {
        return context.getCategory() != null;
    }

    @Override
    public RecoveryAction onError(ProcessContext context, Exception error) {
        context.setCategory(DEFAULT_CATEGORY);
        return RecoveryAction.CONTINUE;
    }
}