package com.javaee.fileservice.service;

import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.util.DocumentParserUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 文件预览服务 (v3.0)
 * 支持PDF分页预览、Word转HTML预览
 */
@Service
public class FilePreviewService {

    private static final Logger log = LoggerFactory.getLogger(FilePreviewService.class);

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private com.javaee.fileservice.config.MinioConfig minioConfig;

    /**
     * 获取文件预览内容
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     * @return 预览结果
     */
    public PreviewResult getPreview(String fileUuid, Long userId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileUuid);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new RuntimeException("文件不存在或无权限");
        }

        PreviewResult result = new PreviewResult();
        result.setFileUuid(fileUuid);
        result.setFileName(fileInfo.getOriginalName());
        result.setFileType(fileInfo.getFileType());

        try {
            byte[] fileContent = downloadFromMinio(fileInfo.getStoragePath());

            String fileType = fileInfo.getFileType();
            if ("pdf".equalsIgnoreCase(fileType)) {
                result = previewPdf(fileContent, result);
            } else if ("word".equalsIgnoreCase(fileType) ||
                       fileInfo.getOriginalName().endsWith(".docx")) {
                result = previewDocx(fileContent, result);
            } else if ("text".equalsIgnoreCase(fileType) ||
                       fileInfo.getOriginalName().endsWith(".txt") ||
                       fileInfo.getOriginalName().endsWith(".md")) {
                result = previewText(fileContent, result);
            } else {
                result.setContentType("unsupported");
                result.setMessage("不支持此文件类型预览，请下载查看");
            }

            return result;
        } catch (Exception e) {
            log.error("文件预览失败: fileUuid={}", fileUuid, e);
            result.setContentType("error");
            result.setMessage("预览失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * PDF预览 - 分页提取
     */
    private PreviewResult previewPdf(byte[] content, PreviewResult result) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            int totalPages = document.getNumberOfPages();
            result.setTotalPages(totalPages);
            result.setContentType("pdf");

            // 提取所有页面文本
            List<PageContent> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);

                PageContent page = new PageContent();
                page.setPageNumber(i);
                page.setContent(pageText.trim());
                pages.add(page);
            }

            result.setPages(pages);
            log.info("PDF预览完成: totalPages={}", totalPages);
            return result;
        }
    }

    /**
     * Word预览 - 转HTML
     */
    private PreviewResult previewDocx(byte[] content, PreviewResult result) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            result.setContentType("html");

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"doc-preview\">");

            // 提取段落并转为HTML
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    // 根据段落样式确定HTML标签
                    String style = paragraph.getStyle();
                    if (style != null && style.contains("Heading")) {
                        html.append("<h3>").append(escapeHtml(text)).append("</h3>");
                    } else {
                        html.append("<p>").append(escapeHtml(text)).append("</p>");
                    }
                }
            }

            // 提取表格
            for (var table : document.getTables()) {
                html.append("<table class=\"doc-table\">");
                for (var row : table.getRows()) {
                    html.append("<tr>");
                    for (var cell : row.getTableCells()) {
                        html.append("<td>").append(escapeHtml(cell.getText())).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }

            html.append("</div>");
            result.setHtmlContent(html.toString());
            log.info("Word预览完成");
            return result;
        }
    }

    /**
     * 文本预览
     */
    private PreviewResult previewText(byte[] content, PreviewResult result) {
        result.setContentType("text");
        String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);
        result.setTextContent(text);
        return result;
    }

    /**
     * 获取PDF指定页面内容
     */
    public PageContent getPdfPage(String fileUuid, int pageNumber, Long userId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileUuid);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new RuntimeException("文件不存在或无权限");
        }

        try {
            byte[] content = downloadFromMinio(fileInfo.getStoragePath());
            try (PDDocument document = Loader.loadPDF(content)) {
                if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
                    throw new RuntimeException("页码超出范围");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = stripper.getText(document);

                PageContent page = new PageContent();
                page.setPageNumber(pageNumber);
                page.setContent(pageText.trim());
                return page;
            }
        } catch (Exception e) {
            log.error("获取PDF页面失败: fileUuid={}, page={}", fileUuid, pageNumber, e);
            throw new RuntimeException("获取页面失败: " + e.getMessage());
        }
    }

    /**
     * 从MinIO下载文件
     */
    private byte[] downloadFromMinio(String storagePath) throws Exception {
        String objectName = extractObjectName(storagePath);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build()
        ).readAllBytes();
    }

    private String extractObjectName(String storagePath) {
        if (storagePath == null || !storagePath.contains("/")) {
            return storagePath;
        }
        return storagePath.substring(storagePath.indexOf("/") + 1);
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 预览结果
     */
    public static class PreviewResult {
        private String fileUuid;
        private String fileName;
        private String fileType;
        private String contentType; // pdf, html, text, unsupported, error
        private Integer totalPages;
        private List<PageContent> pages;
        private String htmlContent;
        private String textContent;
        private String message;

        public String getFileUuid() { return fileUuid; }
        public void setFileUuid(String fileUuid) { this.fileUuid = fileUuid; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        public List<PageContent> getPages() { return pages; }
        public void setPages(List<PageContent> pages) { this.pages = pages; }
        public String getHtmlContent() { return htmlContent; }
        public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
        public String getTextContent() { return textContent; }
        public void setTextContent(String textContent) { this.textContent = textContent; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 页面内容
     */
    public static class PageContent {
        private Integer pageNumber;
        private String content;

        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}