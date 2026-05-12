package com.javaee.fileservice.controller;

import com.javaee.common.model.Result;
import com.javaee.fileservice.config.RabbitMQConfig;
import com.javaee.fileservice.entity.FileMetadata;
import com.javaee.fileservice.service.FileMetadataService;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.util.RabbitMQUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传、下载、删除、分片等核心接口")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private RabbitMQUtil rabbitMQUtil;

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
            List<FileMetadata> files = fileMetadataService.getFileList(userId, page, size, sortBy, direction);
            long total = fileMetadataService.getFileCount(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", total);
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
            List<FileMetadata> files = fileMetadataService.searchFiles(userId, keyword, page, size);
            long total = fileMetadataService.getSearchFileCount(userId, keyword);
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage(), e);
            return Result.fail("搜索文件失败: " + e.getMessage());
        }
    }

    /**
     * 单文件上传（用户隔离）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "单文件上传", description = "上传单个文件到服务器，文件归属于当前用户")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestHeader("X-User-Id") Long userId) {
        log.info("=== 收到文件上传请求 ===");
        log.info("文件名: {}, 文件大小: {}, 文件类型: {}, 用户ID: {}",
            file.getOriginalFilename(), file.getSize(), file.getContentType(), userId);
        try {
            String fileId = fileService.upload(file, userId);
            log.info("文件上传成功，文件ID: {}, 用户ID: {}", fileId, userId);

            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("fileName", file.getOriginalFilename());
            message.put("fileSize", file.getSize());
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());

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
            // 获取文件元数据（验证用户权限）
            FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
            if (metadata == null) {
                log.warn("文件不存在或不属于用户: fileId={}, userId={}", fileId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(("无权访问该文件").getBytes());
            }

            String fileName = metadata.getOriginalFileName();

            byte[] fileBytes = fileService.download(fileId, userId);
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
            // 获取文件元数据（验证用户权限）
            FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
            if (metadata == null) {
                log.warn("文件不存在或不属于用户: fileId={}, userId={}", fileId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(("无权访问该文件").getBytes());
            }

            String contentType = metadata.getFileType();

            byte[] fileBytes = fileService.download(fileId, userId);
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
     * 文件移动
     */
    @PutMapping("/{fileId}/move")
    @Operation(summary = "文件移动", description = "移动文件到指定目录")
    public Result<Map<String, String>> move(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath) {
        try {
            fileService.move(fileId, targetPath);
            return Result.success(Map.of("message", "文件移动成功"));
        } catch (Exception e) {
            return Result.fail("文件移动失败: " + e.getMessage());
        }
    }

    /**
     * 文件复制
     */
    @PutMapping("/{fileId}/copy")
    @Operation(summary = "文件复制", description = "复制文件到指定目录")
    public Result<Map<String, String>> copy(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath) {
        try {
            String newFileId = fileService.copy(fileId, targetPath);
            return Result.success(Map.of("fileId", newFileId, "message", "文件复制成功"));
        } catch (Exception e) {
            return Result.fail("文件复制失败: " + e.getMessage());
        }
    }

}