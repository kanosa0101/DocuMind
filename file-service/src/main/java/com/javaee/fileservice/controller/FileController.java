package com.javaee.fileservice.controller;

import com.javaee.common.model.Result;
import com.javaee.common.util.RabbitMQUtil;
import com.javaee.fileservice.config.RabbitMQConfig;
import com.javaee.fileservice.dto.SimilarityCheckDTO;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.dto.FileStatsDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.service.FileInfoService;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.service.FileSimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件核心接口控制器
 * 所有接口都需要 X-User-Id 请求头，实现用户文件隔离
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传、下载、删除、分片等核心接口")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private RabbitMQUtil rabbitMQUtil;

    @Autowired
    private FileSimilarityService fileSimilarityService;

    /**
     * 获取用户文件列表（带总数，用户隔离）
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户文件列表", description = "分页获取当前用户的文件列表，返回总数用于分页")
    public Result<Map<String, Object>> getFileList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.getByUserId(userId);
            FileStatsDTO stats = fileInfoService.getStats(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", stats.getTotalFiles());
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.fail("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索用户文件（带总数，用户隔离）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户文件", description = "根据关键词搜索当前用户的文件，返回总数用于分页")
    public Result<Map<String, Object>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.search(userId, keyword);
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", files.size());
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage(), e);
            return Result.fail("搜索文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个文件详情（包含AI分析结果）
     */
    @GetMapping("/{fileUuid}")
    @Operation(summary = "获取文件详情", description = "根据fileUuid获取文件信息，包含AI摘要、关键词、分类等")
    public Result<FileInfo> getFileInfo(
            @Parameter(description = "文件UUID") @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileInfo fileInfo = fileInfoService.getByUuid(fileUuid, userId);
            if (fileInfo == null) {
                return Result.fail("文件不存在或无权限访问");
            }
            log.info("获取文件详情: fileUuid={}, userId={}, hasSummary={}",
                    fileUuid, userId, fileInfo.getSummary() != null);
            return Result.success(fileInfo);
        } catch (Exception e) {
            log.error("获取文件详情失败: fileUuid={}, error={}", fileUuid, e.getMessage(), e);
            return Result.fail("获取文件详情失败: " + e.getMessage());
        }
    }

    /**
     * 单文件上传（用户隔离）
     * v3.0: 支持docId参数用于更新现有文件版本
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "单文件上传", description = "上传单个文件到服务器，支持更新现有文件版本")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "docId", required = false) String docId,
            @RequestParam(value = "changeSummary", required = false) String changeSummary,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("=== 收到文件上传请求 ===");
        log.info("文件名: {}, 文件大小: {}, 文件类型: {}, 用户ID: {}, docId: {}",
            file.getOriginalFilename(), file.getSize(), file.getContentType(), userId, docId);
        try {
            FileInfo fileInfo;
            String action;

            // v3.0: 根据是否有docId决定是新建还是更新版本
            if (docId != null && !docId.isEmpty()) {
                // 更新现有文件版本 - 继承版本历史
                fileInfo = fileInfoService.uploadNewVersion(file, docId, changeSummary, userId);
                action = "UPDATE_VERSION";
                log.info("更新版本模式: fileUuid={}, version={}", docId, fileInfo.getVersion());
            } else {
                // 上传新文件
                fileInfo = fileInfoService.upload(file, userId);
                action = "NEW";
                log.info("新建文件模式: fileUuid={}", fileInfo.getFileUuid());
            }

            String fileId = fileInfo.getFileUuid();

            // 构建MQ消息，触发AI处理
            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("fileName", file.getOriginalFilename());
            message.put("fileSize", file.getSize());
            message.put("userId", userId);
            message.put("action", action);
            message.put("timestamp", LocalDateTime.now().toString());

            if (action.equals("UPDATE_VERSION")) {
                message.put("changeSummary", changeSummary != null ? changeSummary : "");
            }

            log.info("发送文件上传消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_UPLOAD_ROUTING_KEY, message);

            return Result.success(Map.of("fileId", fileId, "message", "文件上传成功"));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 多文件上传（用户隔离）
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "多文件上传", description = "上传多个文件到服务器，文件归属于当前用户")
    public Result<Map<String, Object>> uploadMultiple(@RequestParam("files") MultipartFile[] files,
                                                       @RequestHeader("X-User-Id") Long userId) {
        log.info("=== 收到多文件上传请求 ===");
        log.info("文件数量: {}, 用户ID: {}", files.length, userId);
        try {
            log.info("调用fileService.uploadMultiple方法");
            String[] fileIds = fileService.uploadMultiple(files, userId);
            log.info("多文件上传成功，文件ID数量: {}", fileIds.length);
            return Result.success(Map.of("fileIds", fileIds, "message", "文件上传成功", "count", fileIds.length));
        } catch (Exception e) {
            log.error("多文件上传失败: {}", e.getMessage(), e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传（用户隔离）
     */
    @PostMapping(value = "/upload-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "分片上传", description = "大文件分片上传")
    public Result<Map<String, String>> uploadChunk(@RequestParam("chunk") MultipartFile chunk,
                           @RequestParam("fileId") String fileId,
                           @RequestParam("chunkIndex") int chunkIndex,
                           @RequestParam("totalChunks") int totalChunks,
                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            fileService.uploadChunk(chunk, fileId, chunkIndex, totalChunks);
            return Result.success(Map.of("message", "分片上传成功", "chunkIndex", String.valueOf(chunkIndex)));
        } catch (Exception e) {
            return Result.fail("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 分片合并（用户隔离）
     */
    @PostMapping("/merge-chunk")
    @Operation(summary = "分片合并", description = "合并文件分片，文件归属于当前用户")
    public Result<Map<String, String>> mergeChunk(@Parameter(description = "文件唯一标识") @RequestParam("fileId") String fileId,
                          @Parameter(description = "文件名") @RequestParam("fileName") String fileName,
                          @RequestHeader("X-User-Id") Long userId) {
        try {
            String mergedFileId = fileService.mergeChunk(fileId, fileName, userId);
            return Result.success(Map.of("fileId", mergedFileId, "message", "文件合并成功"));
        } catch (Exception e) {
            return Result.fail("文件合并失败: " + e.getMessage());
        }
    }

    /**
     * 文件下载（用户隔离）
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "文件下载", description = "下载当前用户拥有的指定文件")
    public ResponseEntity<byte[]> download(@Parameter(description = "文件ID") @PathVariable String fileId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            // 获取文件信息（验证用户权限）
            FileInfo fileInfo = fileInfoService.getByUuid(fileId, userId);
            if (fileInfo == null) {
                log.warn("文件不存在或不属于用户: fileId={}, userId={}", fileId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(("无权访问该文件").getBytes());
            }

            String fileName = fileInfo.getOriginalName();
            byte[] fileBytes = fileInfoService.download(fileId, userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 使用RFC 5987编码格式支持中文文件名
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());

            log.info("发送文件下载消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_DOWNLOAD_ROUTING_KEY, message);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("文件下载失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 文件预览（用户隔离）
     */
    @GetMapping("/preview/{fileId}")
    @Operation(summary = "文件预览", description = "预览当前用户拥有的指定文件")
    public ResponseEntity<byte[]> preview(@Parameter(description = "文件ID") @PathVariable String fileId,
                                          @RequestHeader("X-User-Id") Long userId) {
        try {
            // 获取文件信息（验证用户权限）
            FileInfo fileInfo = fileInfoService.getByUuid(fileId, userId);
            if (fileInfo == null) {
                log.warn("文件不存在或不属于用户: fileId={}, userId={}", fileId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(("无权访问该文件").getBytes());
            }

            // v3.0 fix: contentType为空时，根据fileName推断
            String contentType = fileInfo.getFileType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = inferContentType(fileInfo.getFileName());
            }

            byte[] fileBytes = fileInfoService.download(fileId, userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("文件预览失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("文件预览失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 根据文件名推断contentType
     */
    private String inferContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    /**
     * 文件删除（用户隔离）
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "文件删除", description = "删除当前用户拥有的指定文件")
    public Result<Map<String, String>> delete(@Parameter(description = "文件ID") @PathVariable String fileId,
                                                      @RequestHeader("X-User-Id") Long userId) {
        try {
            fileService.delete(fileId, userId);

            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());

            log.info("发送文件删除消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_DELETE_ROUTING_KEY, message);

            return Result.success(Map.of("message", "文件删除成功"));
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return Result.fail("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 文件重命名（用户隔离）
     */
    @PutMapping("/{fileId}/rename")
    @Operation(summary = "文件重命名", description = "重命名当前用户拥有的指定文件")
    public Result<Map<String, String>> rename(@Parameter(description = "文件ID") @PathVariable String fileId,
                      @Parameter(description = "新文件名") @RequestParam("newName") String newName,
                      @RequestHeader("X-User-Id") Long userId) {
        try {
            fileService.rename(fileId, newName, userId);
            return Result.success(Map.of("message", "文件重命名成功"));
        } catch (Exception e) {
            return Result.fail("文件重命名失败: " + e.getMessage());
        }
    }

    /**
     * 文件移动（用户隔离）
     */
    @PutMapping("/{fileId}/move")
    @Operation(summary = "文件移动", description = "移动当前用户拥有的文件到指定目录")
    public Result<Map<String, String>> move(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath,
                    @RequestHeader("X-User-Id") Long userId) {
        try {
            // 验证文件所有权
            FileInfo fileInfo = fileInfoService.getByUuid(fileId, userId);
            if (fileInfo == null) {
                return Result.fail("无权操作该文件或文件不存在");
            }
            fileService.move(fileId, targetPath, userId);
            return Result.success(Map.of("message", "文件移动成功"));
        } catch (Exception e) {
            return Result.fail("文件移动失败: " + e.getMessage());
        }
    }

    /**
     * 文件复制（用户隔离）
     */
    @PutMapping("/{fileId}/copy")
    @Operation(summary = "文件复制", description = "复制当前用户拥有的文件到指定目录，新文件归属于当前用户")
    public Result<Map<String, String>> copy(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath,
                    @RequestHeader("X-User-Id") Long userId) {
        try {
            // 验证文件所有权
            FileInfo fileInfo = fileInfoService.getByUuid(fileId, userId);
            if (fileInfo == null) {
                return Result.fail("无权操作该文件或文件不存在");
            }
            String newFileId = fileService.copy(fileId, targetPath, userId);
            return Result.success(Map.of("fileId", newFileId, "message", "文件复制成功"));
        } catch (Exception e) {
            return Result.fail("文件复制失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件统计（用户隔离）
     */
    @GetMapping("/stats")
    @Operation(summary = "获取用户文件统计", description = "获取当前用户的文件数量和存储用量")
    public Result<Map<String, Object>> getFileStats(@RequestHeader("X-User-Id") Long userId) {
        try {
            FileStatsDTO stats = fileInfoService.getStats(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("fileCount", stats.getTotalFiles());
            result.put("totalSize", stats.getTotalSize());
            result.put("totalSizeMB", Math.round(stats.getTotalSize() / 1024.0 / 1024.0));
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文件统计失败: {}", e.getMessage(), e);
            return Result.fail("获取文件统计失败: " + e.getMessage());
        }
    }

    /**
     * 文件相似度检测（v2.0新增）
     * 用于上传时判断是否为新版本
     * 使用POST请求避免中文编码问题
     */
    @PostMapping("/similarity/check")
    @Operation(summary = "文件相似度检测", description = "检测用户已有文件中是否存在相似文件，用于版本识别")
    public Result<SimilarityResultDTO> checkSimilarity(
            @RequestBody SimilarityCheckDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            log.info("检测文件相似度: fileName={}, fileType={}, userId={}", request.getFileName(), request.getFileType(), userId);
            SimilarityResultDTO result = fileSimilarityService.detectSimilarity(request.getFileName(), request.getFileType(), userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("文件相似度检测失败: {}", e.getMessage(), e);
            return Result.fail("文件相似度检测失败: " + e.getMessage());
        }
    }

    /**
     * 更新文件版本信息（v2.0新增）
     * 用于document-service在创建文档后更新文件的versionOfDoc字段
     * v3.0: 该方法已废弃，版本信息现在内嵌在FileInfo中
     */
    @PutMapping("/{fileId}/version-info")
    @Operation(summary = "更新文件版本信息", description = "已废弃，v3.0版本信息内嵌在FileInfo中")
    @Deprecated(since = "3.0", forRemoval = true)
    public Result<Map<String, String>> updateVersionInfo(
            @PathVariable String fileId,
            @RequestParam("docId") String docId,
            @RequestParam("versionNum") Integer versionNum,
            @RequestParam(value = "previousFileId", required = false) String previousFileId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            log.info("更新文件版本信息: fileId={}, docId={}, versionNum={}, previousFileId={}", fileId, docId, versionNum, previousFileId);
            // v3.0: 版本信息内嵌在FileInfo中，不再需要单独更新
            return Result.success(Map.of("message", "该方法已废弃，请使用v3.0 API"));
        } catch (Exception e) {
            log.error("更新文件版本信息失败: {}", e.getMessage(), e);
            return Result.fail("更新文件版本信息失败: " + e.getMessage());
        }
    }

}