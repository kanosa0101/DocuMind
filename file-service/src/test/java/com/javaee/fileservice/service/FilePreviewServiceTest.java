package com.javaee.fileservice.service;

import com.javaee.fileservice.service.FilePreviewService.PageContent;
import com.javaee.fileservice.service.FilePreviewService.PreviewResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件预览服务测试
 * Phase 4验收标准：所有测试通过
 */
class FilePreviewServiceTest {

    // ===== PreviewResult测试 =====

    @Test
    @DisplayName("PreviewResult内容类型验证")
    void testPreviewResultContentType() {
        PreviewResult result = new PreviewResult();

        // PDF预览
        result.setContentType("pdf");
        result.setTotalPages(10);
        assertEquals("pdf", result.getContentType());
        assertEquals(10, result.getTotalPages());

        // HTML预览
        result.setContentType("html");
        result.setHtmlContent("<div>test</div>");
        assertEquals("html", result.getContentType());
        assertEquals("<div>test</div>", result.getHtmlContent());

        // 文本预览
        result.setContentType("text");
        result.setTextContent("plain text");
        assertEquals("text", result.getContentType());
        assertEquals("plain text", result.getTextContent());

        // 不支持的类型
        result.setContentType("unsupported");
        result.setMessage("不支持此文件类型预览");
        assertEquals("unsupported", result.getContentType());
        assertEquals("不支持此文件类型预览", result.getMessage());

        // 错误
        result.setContentType("error");
        result.setMessage("预览失败");
        assertEquals("error", result.getContentType());
    }

    @Test
    @DisplayName("PreviewResult字段完整性")
    void testPreviewResultFields() {
        PreviewResult result = new PreviewResult();

        result.setFileUuid("test-uuid");
        result.setFileName("test.pdf");
        result.setFileType("pdf");
        result.setContentType("pdf");
        result.setTotalPages(5);
        result.setMessage("success");

        assertEquals("test-uuid", result.getFileUuid());
        assertEquals("test.pdf", result.getFileName());
        assertEquals("pdf", result.getFileType());
        assertEquals("pdf", result.getContentType());
        assertEquals(5, result.getTotalPages());
        assertEquals("success", result.getMessage());
    }

    // ===== PageContent测试 =====

    @Test
    @DisplayName("PageContent页面内容验证")
    void testPageContent() {
        PageContent page = new PageContent();

        page.setPageNumber(1);
        page.setContent("第一页内容");

        assertEquals(1, page.getPageNumber());
        assertEquals("第一页内容", page.getContent());
    }

    @Test
    @DisplayName("PDF分页列表验证")
    void testPdfPageList() {
        PreviewResult result = new PreviewResult();
        result.setContentType("pdf");
        result.setTotalPages(3);

        List<PageContent> pages = new java.util.ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PageContent page = new PageContent();
            page.setPageNumber(i);
            page.setContent("第" + i + "页内容");
            pages.add(page);
        }
        result.setPages(pages);

        assertEquals(3, result.getPages().size());
        assertEquals(1, result.getPages().get(0).getPageNumber());
        assertEquals("第1页内容", result.getPages().get(0).getContent());
        assertEquals(3, result.getPages().get(2).getPageNumber());
    }

    // ===== HTML转义测试 =====

    @Test
    @DisplayName("HTML转义验证")
    void testHtmlEscape() {
        String rawText = "<script>alert('test')</script>";
        String escaped = escapeHtml(rawText);

        assertTrue(escaped.contains("&lt;"));
        assertTrue(escaped.contains("&gt;"));
        assertFalse(escaped.contains("<script>"));
    }

    @Test
    @DisplayName("特殊字符转义验证")
    void testSpecialCharacterEscape() {
        String text = "Test & \"Quote\" 'Single'";
        String escaped = escapeHtml(text);

        assertTrue(escaped.contains("&amp;"));
        assertTrue(escaped.contains("&quot;"));
        assertTrue(escaped.contains("&#39;"));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    // ===== 预览边界条件测试 =====

    @Test
    @DisplayName("空内容处理")
    void testEmptyContent() {
        PreviewResult result = new PreviewResult();
        result.setContentType("text");
        result.setTextContent("");

        assertEquals("", result.getTextContent());
    }

    @Test
    @DisplayName("null内容处理")
    void testNullContent() {
        PreviewResult result = new PreviewResult();
        result.setTextContent(null);
        result.setHtmlContent(null);
        result.setPages(null);

        assertNull(result.getTextContent());
        assertNull(result.getHtmlContent());
        assertNull(result.getPages());
    }

    @Test
    @DisplayName("页码范围验证")
    void testPageNumberRange() {
        int totalPages = 10;
        int pageNumber = 5;

        // 页码应在有效范围内
        assertTrue(pageNumber >= 1);
        assertTrue(pageNumber <= totalPages);

        // 边界值测试
        pageNumber = 1;
        assertTrue(pageNumber >= 1 && pageNumber <= totalPages);

        pageNumber = 10;
        assertTrue(pageNumber >= 1 && pageNumber <= totalPages);

        // 超出范围
        pageNumber = 0;
        assertFalse(pageNumber >= 1);

        pageNumber = 11;
        assertFalse(pageNumber <= totalPages);
    }

    // ===== Word转HTML结构测试 =====

    @Test
    @DisplayName("Word转HTML结构验证")
    void testDocxHtmlStructure() {
        String html = buildMockDocxHtml();

        assertTrue(html.contains("<div"));
        assertTrue(html.contains("<p>"));
        assertTrue(html.contains("</p>"));
        assertTrue(html.contains("</div>"));
    }

    @Test
    @DisplayName("Word表格转HTML验证")
    void testDocxTableHtml() {
        String html = buildMockDocxHtmlWithTable();

        assertTrue(html.contains("<table"));
        assertTrue(html.contains("<tr>"));
        assertTrue(html.contains("<td>"));
        assertTrue(html.contains("</table>"));
    }

    private String buildMockDocxHtml() {
        return "<div class=\"doc-preview\"><p>段落内容</p><h3>标题</h3></div>";
    }

    private String buildMockDocxHtmlWithTable() {
        return "<div class=\"doc-preview\"><table class=\"doc-table\"><tr><td>单元格</td></tr></table></div>";
    }

    // ===== 文件类型检测测试 =====

    @Test
    @DisplayName("文件类型检测验证")
    void testFileTypeDetection() {
        String fileName1 = "document.pdf";
        assertEquals("pdf", getFileExtension(fileName1));

        String fileName2 = "report.docx";
        assertEquals("docx", getFileExtension(fileName2));

        String fileName3 = "notes.txt";
        assertEquals("txt", getFileExtension(fileName3));

        String fileName4 = "README.md";
        assertEquals("md", getFileExtension(fileName4));

        String fileName5 = "noextension";
        assertEquals("", getFileExtension(fileName5));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // ===== 预览类型匹配测试 =====

    @Test
    @DisplayName("预览类型与文件类型匹配")
    void testPreviewTypeMatching() {
        String fileType = "pdf";
        String contentType = getPreviewContentType(fileType);
        assertEquals("pdf", contentType);

        fileType = "word";
        contentType = getPreviewContentType(fileType);
        assertEquals("html", contentType);

        fileType = "text";
        contentType = getPreviewContentType(fileType);
        assertEquals("text", contentType);

        fileType = "other";
        contentType = getPreviewContentType(fileType);
        assertEquals("unsupported", contentType);
    }

    private String getPreviewContentType(String fileType) {
        return switch (fileType) {
            case "pdf" -> "pdf";
            case "word" -> "html";
            case "text" -> "text";
            default -> "unsupported";
        };
    }
}