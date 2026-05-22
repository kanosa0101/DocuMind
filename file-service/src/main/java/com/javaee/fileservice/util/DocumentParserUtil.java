package com.javaee.fileservice.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析工具类 (v3.0)
 * 使用Apache POI解析各种文档格式
 */
public class DocumentParserUtil {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserUtil.class);

    /**
     * 解析文档内容
     * @param fileContent 文件内容字节数组
     * @param fileName 文件名（用于判断格式）
     * @return 解析后的文本内容
     */
    public static String parseDocument(byte[] fileContent, String fileName) {
        if (fileContent == null || fileContent.length == 0) {
            return "";
        }

        String extension = getFileExtension(fileName);
        log.info("开始解析文档: fileName={}, extension={}, size={}", fileName, extension, fileContent.length);

        try {
            return switch (extension.toLowerCase()) {
                case "docx" -> parseDocx(fileContent);
                case "doc" -> parseDoc(fileContent);
                case "txt" -> parseTxt(fileContent);
                case "pdf" -> parsePdf(fileContent);
                case "md" -> parseTxt(fileContent);
                case "xml" -> parseTxt(fileContent);
                default -> {
                    log.warn("不支持的文件格式: {}", extension);
                    yield parseTxt(fileContent);
                }
            };
        } catch (Exception e) {
            log.error("解析文档失败: {}", fileName, e);
            // 如果解析失败，尝试作为纯文本处理
            try {
                return new String(fileContent, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 解析DOCX文件
     */
    private static String parseDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder text = new StringBuilder();

            // 解析段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }

            // 解析表格
            for (var table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        text.append(cell.getText()).append("\t");
                    }
                    text.append("\n");
                }
            }

            return text.toString().trim();
        }
    }

    /**
     * 解析DOC文件（旧版Word）
     */
    private static String parseDoc(byte[] content) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(content))) {
            WordExtractor extractor = new WordExtractor(document);
            return extractor.getText().trim();
        }
    }

    /**
     * 解析TXT文件
     */
    private static String parseTxt(byte[] content) {
        return new String(content, StandardCharsets.UTF_8).trim();
    }

    /**
     * 解析PDF文件
     * 使用PDFBox提取文本，并过滤乱码字符
     */
    private static String parsePdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");

            String text = stripper.getText(document);

            if (text == null || text.isEmpty()) {
                log.warn("PDF解析结果为空");
                return "PDF文件，无法提取文本内容";
            }

            // 过滤乱码和空白字符
            text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
            text = text.replaceAll(" {3,}", " ");
            text = text.replaceAll("\\n{3,}", "\n\n");

            // 检查文本是否有效
            int readableChars = 0;
            for (char c : text.toCharArray()) {
                if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) ||
                    c == '.' || c == ',' || c == '!' || c == '?' || c == ';' || c == ':') {
                    readableChars++;
                }
            }

            double readableRatio = (double) readableChars / text.length();
            if (readableRatio < 0.3) {
                log.warn("PDF文本可读比例过低: {}%，可能为扫描版PDF", (int)(readableRatio * 100));
                return "PDF文件（扫描版或图像版），无法提取文本内容";
            }

            return text.trim();
        } catch (Exception e) {
            log.error("PDF解析异常: {}", e.getMessage());
            return "PDF文件，解析失败: " + e.getMessage();
        }
    }

    /**
     * 清理解析后的文本（去除多余空白）
     */
    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\n+", "\n")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * 获取文档摘要
     */
    public static String getSummary(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = cleanText(text);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}