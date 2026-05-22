package com.javaee.fileservice.controller;

import com.javaee.common.model.Result;
import com.javaee.fileservice.dto.FileInfoVO;
import com.javaee.fileservice.dto.FileStatsDTO;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.entity.VersionHistoryItem;
import com.javaee.fileservice.service.FileInfoService;
import com.javaee.fileservice.service.FilePreviewService;
import com.javaee.fileservice.service.FilePreviewService.PreviewResult;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件管理控制器 (v3.0)
 * 统一文件管理接口，整合文件上传、版本管理、删除恢复、分享等功能
 */
@RestController
@RequestMapping("/api/v3/files")
@Tag(name = "文件管理v3.0", description = "统一文件管理接口 - 文件即一切架构")
public class FileInfoController {

    private static final Logger log = LoggerFactory.getLogger(FileInfoController.class);

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private FilePreviewService filePreviewService;

    // ===== 上传相关 =====

    /**
     * 上传新文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传新文件", description = "上传单个文件，自动创建FileInfo并触发AI处理")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("上传新文件: fileName={}, userId={}", file.getOriginalFilename(), userId);
        try {
            FileInfo fileInfo = fileInfoService.upload(file, userId);
            return Result.success(Map.of(
                    "fileUuid", fileInfo.getFileUuid(),
                    "fileName", fileInfo.getOriginalName(),
                    "fileSize", fileInfo.getFileSize(),
                    "message", "文件上传成功"
            ));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传新版本
     */
    @PostMapping(value = "/upload-version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传新版本", description = "上传文件的更新版本")
    public Result<Map<String, Object>> uploadNewVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileUuid") String fileUuid,
            @RequestParam(value = "changeSummary", required = false) String changeSummary,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("上传新版本: fileUuid={}, userId={}", fileUuid, userId);
        try {
            FileInfo fileInfo = fileInfoService.uploadNewVersion(file, fileUuid, changeSummary, userId);
            return Result.success(Map.of(
                    "fileUuid", fileInfo.getFileUuid(),
                    "version", fileInfo.getVersion(),
                    "message", "新版本上传成功"
            ));
        } catch (Exception e) {
            log.error("上传新版本失败: {}", e.getMessage(), e);
            return Result.fail("上传新版本失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量上传", description = "上传多个文件")
    public Result<Map<String, Object>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("批量上传: count={}, userId={}", files.length, userId);
        try {
            List<FileInfo> fileInfos = fileInfoService.uploadMultiple(files, userId);
            return Result.success(Map.of(
                    "count", fileInfos.size(),
                    "files", fileInfos.stream().map(f -> Map.of(
                            "fileUuid", f.getFileUuid(),
                            "fileName", f.getOriginalName()
                    )).toList(),
                    "message", "批量上传成功"
            ));
        } catch (Exception e) {
            log.error("批量上传失败: {}", e.getMessage(), e);
            return Result.fail("批量上传失败: " + e.getMessage());
        }
    }

    // ===== 查询相关 =====

    /**
     * 获取文件详情
     */
    @GetMapping("/{fileUuid}")
    @Operation(summary = "获取文件详情", description = "根据fileUuid获取文件详情")
    public Result<FileInfoVO> getFile(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileInfo fileInfo = fileInfoService.getByUuid(fileUuid, userId);
            if (fileInfo == null) {
                return Result.fail("文件不存在或无权限");
            }
            return Result.success(FileInfoVO.fromFileInfo(fileInfo));
        } catch (Exception e) {
            log.error("获取文件详情失败: {}", e.getMessage(), e);
            return Result.fail("获取文件详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户文件列表", description = "获取当前用户的所有活跃文件")
    public Result<List<FileInfoVO>> getFileList(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.getByUserId(userId);
            List<FileInfoVO> vos = files.stream().map(FileInfoVO::fromFileInfo).collect(Collectors.toList());
            return Result.success(vos);
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.fail("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取回收站文件列表
     */
    @GetMapping("/deleted")
    @Operation(summary = "获取回收站文件", description = "获取用户已删除的文件列表")
    public Result<List<FileInfoVO>> getDeletedFiles(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.getDeletedByUserId(userId);
            List<FileInfoVO> vos = files.stream().map(FileInfoVO::fromFileInfo).collect(Collectors.toList());
            return Result.success(vos);
        } catch (Exception e) {
            log.error("获取回收站文件失败: {}", e.getMessage(), e);
            return Result.fail("获取回收站文件失败: " + e.getMessage());
        }
    }

    /**
     * 根据分类获取文件
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类获取文件", description = "获取指定分类的文件列表")
    public Result<List<FileInfoVO>> getByCategory(
            @PathVariable String category,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.getByCategory(userId, category);
            List<FileInfoVO> vos = files.stream().map(FileInfoVO::fromFileInfo).collect(Collectors.toList());
            return Result.success(vos);
        } catch (Exception e) {
            log.error("获取分类文件失败: {}", e.getMessage(), e);
            return Result.fail("获取分类文件失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    @Operation(summary = "搜索文件", description = "根据关键词搜索文件")
    public Result<List<FileInfoVO>> searchFiles(
            @RequestParam String keyword,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.search(userId, keyword);
            List<FileInfoVO> vos = files.stream().map(FileInfoVO::fromFileInfo).collect(Collectors.toList());
            return Result.success(vos);
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage(), e);
            return Result.fail("搜索文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取文件统计", description = "获取用户文件的统计数据")
    public Result<FileStatsDTO> getStats(@RequestHeader("X-User-Id") Long userId) {
        try {
            FileStatsDTO stats = fileInfoService.getStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取文件统计失败: {}", e.getMessage(), e);
            return Result.fail("获取文件统计失败: " + e.getMessage());
        }
    }

    // ===== 版本管理 =====

    /**
     * 获取版本历史
     */
    @GetMapping("/{fileUuid}/versions")
    @Operation(summary = "获取版本历史", description = "获取文件的版本历史列表")
    public Result<List<VersionHistoryItem>> getVersionHistory(@PathVariable String fileUuid) {
        try {
            List<VersionHistoryItem> history = fileInfoService.getVersionHistory(fileUuid);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取版本历史失败: {}", e.getMessage(), e);
            return Result.fail("获取版本历史失败: " + e.getMessage());
        }
    }

    /**
     * 切换版本
     */
    @PutMapping("/{fileUuid}/version/{targetVersion}")
    @Operation(summary = "切换版本", description = "切换到指定的历史版本")
    public Result<FileInfoVO> switchVersion(
            @PathVariable String fileUuid,
            @PathVariable Integer targetVersion,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("切换版本: fileUuid={}, targetVersion={}, userId={}", fileUuid, targetVersion, userId);
        try {
            FileInfo fileInfo = fileInfoService.switchVersion(fileUuid, targetVersion, userId);
            return Result.success(FileInfoVO.fromFileInfo(fileInfo));
        } catch (Exception e) {
            log.error("切换版本失败: {}", e.getMessage(), e);
            return Result.fail("切换版本失败: " + e.getMessage());
        }
    }

    /**
     * 下载指定版本
     */
    @GetMapping("/{fileUuid}/download/{version}")
    @Operation(summary = "下载指定版本", description = "下载指定版本的文件")
    public ResponseEntity<byte[]> downloadVersion(
            @PathVariable String fileUuid,
            @PathVariable Integer version,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileInfo fileInfo = fileInfoService.getByUuid(fileUuid, userId);
            if (fileInfo == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("无权访问该文件".getBytes());
            }

            byte[] fileBytes = fileInfoService.downloadVersion(fileUuid, version, userId);
            String fileName = fileInfo.getOriginalName();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("下载文件失败: " + e.getMessage()).getBytes());
        }
    }

    // ===== 删除恢复 =====

    /**
     * 软删除文件
     */
    @DeleteMapping("/{fileUuid}")
    @Operation(summary = "软删除文件", description = "将文件移入回收站")
    public Result<Map<String, String>> softDelete(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("软删除文件: fileUuid={}, userId={}", fileUuid, userId);
        try {
            fileInfoService.softDelete(fileUuid, userId);
            return Result.success(Map.of("message", "文件已移入回收站"));
        } catch (Exception e) {
            log.error("软删除失败: {}", e.getMessage(), e);
            return Result.fail("软删除失败: " + e.getMessage());
        }
    }

    /**
     * 恢复文件
     */
    @PutMapping("/{fileUuid}/restore")
    @Operation(summary = "恢复文件", description = "从回收站恢复文件")
    public Result<Map<String, String>> restore(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("恢复文件: fileUuid={}, userId={}", fileUuid, userId);
        try {
            fileInfoService.restore(fileUuid, userId);
            return Result.success(Map.of("message", "文件已恢复"));
        } catch (Exception e) {
            log.error("恢复文件失败: {}", e.getMessage(), e);
            return Result.fail("恢复文件失败: " + e.getMessage());
        }
    }

    /**
     * 永久删除
     */
    @DeleteMapping("/{fileUuid}/permanent")
    @Operation(summary = "永久删除", description = "永久删除文件，不可恢复")
    public Result<Map<String, String>> permanentDelete(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("永久删除文件: fileUuid={}, userId={}", fileUuid, userId);
        try {
            fileInfoService.permanentDelete(fileUuid, userId);
            return Result.success(Map.of("message", "文件已永久删除"));
        } catch (Exception e) {
            log.error("永久删除失败: {}", e.getMessage(), e);
            return Result.fail("永久删除失败: " + e.getMessage());
        }
    }

    // ===== 批量操作 =====

    /**
     * 批量删除
     */
    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除", description = "批量软删除多个文件")
    public Result<Map<String, String>> batchDelete(
            @RequestBody List<String> fileUuids,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("批量删除: count={}, userId={}", fileUuids.size(), userId);
        try {
            fileInfoService.batchDelete(fileUuids, userId);
            return Result.success(Map.of("message", "批量删除成功", "count", String.valueOf(fileUuids.size())));
        } catch (Exception e) {
            log.error("批量删除失败: {}", e.getMessage(), e);
            return Result.fail("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量修改分类
     */
    @PostMapping("/batch/classify")
    @Operation(summary = "批量修改分类", description = "批量修改文件分类")
    public Result<Map<String, String>> batchClassify(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<String> fileUuids = (List<String>) request.get("fileUuids");
            String category = (String) request.get("category");
            log.info("批量修改分类: count={}, category={}, userId={}", fileUuids.size(), category, userId);
            fileInfoService.batchClassify(fileUuids, category, userId);
            return Result.success(Map.of("message", "批量分类成功"));
        } catch (Exception e) {
            log.error("批量分类失败: {}", e.getMessage(), e);
            return Result.fail("批量分类失败: " + e.getMessage());
        }
    }

    // ===== 修改相关 =====

    /**
     * 更新分类
     */
    @PutMapping("/{fileUuid}/category")
    @Operation(summary = "更新分类", description = "修改文件分类")
    public Result<Map<String, String>> updateCategory(
            @PathVariable String fileUuid,
            @RequestParam String category,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            fileInfoService.updateCategory(fileUuid, category, userId);
            return Result.success(Map.of("message", "分类更新成功"));
        } catch (Exception e) {
            log.error("更新分类失败: {}", e.getMessage(), e);
            return Result.fail("更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新AI结果（内部调用）
     */
    @PutMapping("/{fileUuid}/ai-result")
    @Operation(summary = "更新AI结果", description = "AI服务处理完成后更新结果")
    public Result<Map<String, String>> updateAiResult(
            @PathVariable String fileUuid,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "category", required = false) String category,
            @RequestHeader(value = "X-Internal-Call", required = false) String internalCall) {
        try {
            fileInfoService.updateAiResult(fileUuid, summary, keywords, category);
            return Result.success(Map.of("message", "AI结果更新成功"));
        } catch (Exception e) {
            log.error("更新AI结果失败: {}", e.getMessage(), e);
            return Result.fail("更新AI结果失败: " + e.getMessage());
        }
    }

    /**
     * 更新处理状态（内部调用）
     */
    @PutMapping("/{fileUuid}/status")
    @Operation(summary = "更新处理状态", description = "更新AI处理状态")
    public Result<Map<String, String>> updateProcessStatus(
            @PathVariable String fileUuid,
            @RequestParam String status,
            @RequestHeader(value = "X-Internal-Call", required = false) String internalCall) {
        try {
            fileInfoService.updateProcessStatus(fileUuid, status);
            return Result.success(Map.of("message", "状态更新成功"));
        } catch (Exception e) {
            log.error("更新状态失败: {}", e.getMessage(), e);
            return Result.fail("更新状态失败: " + e.getMessage());
        }
    }

    // ===== 下载相关 =====

    /**
     * 下载文件
     */
    @GetMapping("/{fileUuid}/download")
    @Operation(summary = "下载文件", description = "下载当前版本的文件")
    public ResponseEntity<byte[]> download(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileInfo fileInfo = fileInfoService.getByUuid(fileUuid, userId);
            if (fileInfo == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("无权访问该文件".getBytes());
            }

            byte[] fileBytes = fileInfoService.download(fileUuid, userId);
            String fileName = fileInfo.getOriginalName();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("下载文件失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 获取预览URL
     */
    @GetMapping("/{fileUuid}/preview")
    @Operation(summary = "获取预览URL", description = "获取文件的预览链接")
    public Result<Map<String, String>> getPreviewUrl(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            String previewUrl = fileInfoService.getPreviewUrl(fileUuid, userId);
            return Result.success(Map.of("previewUrl", previewUrl));
        } catch (Exception e) {
            log.error("获取预览URL失败: {}", e.getMessage(), e);
            return Result.fail("获取预览URL失败: " + e.getMessage());
        }
    }

    // ===== 分享相关 =====

    /**
     * 分享给其他用户
     */
    @PostMapping("/{fileUuid}/share")
    @Operation(summary = "分享给用户", description = "将文件分享给指定用户")
    public Result<Map<String, String>> shareToUsers(
            @PathVariable String fileUuid,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<Long> shareToIds = (List<Long>) request.get("shareToIds");
            String permission = (String) request.getOrDefault("permission", "VIEW");
            Integer expireDays = request.get("expireDays") != null ? (Integer) request.get("expireDays") : null;

            fileInfoService.shareToUsers(fileUuid, shareToIds, permission, expireDays, userId);
            return Result.success(Map.of("message", "分享成功"));
        } catch (Exception e) {
            log.error("分享失败: {}", e.getMessage(), e);
            return Result.fail("分享失败: " + e.getMessage());
        }
    }

    /**
     * 创建公开分享链接
     */
    @PostMapping("/{fileUuid}/share-public")
    @Operation(summary = "创建公开分享", description = "创建公开分享链接")
    public Result<Map<String, String>> createPublicShare(
            @PathVariable String fileUuid,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            Integer expireDays = request != null && request.get("expireDays") != null ? (Integer) request.get("expireDays") : null;
            String password = request != null ? (String) request.get("password") : null;
            Integer downloadLimit = request != null && request.get("downloadLimit") != null ? (Integer) request.get("downloadLimit") : null;

            String shareCode = fileInfoService.createPublicShare(fileUuid, expireDays, password, downloadLimit, userId);
            return Result.success(Map.of("shareCode", shareCode, "message", "公开分享创建成功"));
        } catch (Exception e) {
            log.error("创建公开分享失败: {}", e.getMessage(), e);
            return Result.fail("创建公开分享失败: " + e.getMessage());
        }
    }

    /**
     * 获取公开分享的文件
     */
    @GetMapping("/public/{shareCode}")
    @Operation(summary = "获取公开分享文件", description = "通过分享码获取公开分享的文件信息")
    public Result<FileInfoVO> getPublicSharedFile(
            @PathVariable String shareCode,
            @RequestParam(required = false) String password) {
        try {
            FileInfo fileInfo = fileInfoService.getPublicSharedFile(shareCode, password);
            return Result.success(FileInfoVO.fromFileInfo(fileInfo));
        } catch (Exception e) {
            log.error("获取公开分享文件失败: {}", e.getMessage(), e);
            return Result.fail("获取公开分享文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享给我的文件
     */
    @GetMapping("/shared-to-me")
    @Operation(summary = "获取分享给我的文件", description = "获取其他用户分享给我的文件列表")
    public Result<List<FileInfoVO>> getSharedToMe(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileInfo> files = fileInfoService.getSharedToMe(userId);
            List<FileInfoVO> vos = files.stream().map(FileInfoVO::fromFileInfo).collect(Collectors.toList());
            return Result.success(vos);
        } catch (Exception e) {
            log.error("获取分享文件失败: {}", e.getMessage(), e);
            return Result.fail("获取分享文件失败: " + e.getMessage());
        }
    }

    // ===== 相似检测 =====

    /**
     * 相似度检测
     */
    @PostMapping("/similarity/check")
    @Operation(summary = "相似度检测", description = "检测文件名相似度，用于版本识别")
    public Result<SimilarityResultDTO> checkSimilarity(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            String fileName = request.get("fileName");
            SimilarityResultDTO result = fileInfoService.checkSimilarity(fileName, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("相似度检测失败: {}", e.getMessage(), e);
            return Result.fail("相似度检测失败: " + e.getMessage());
        }
    }

    // ===== 文件预览 =====

    /**
     * 获取文件预览
     */
    @GetMapping("/{fileUuid}/preview-content")
    @Operation(summary = "获取文件预览内容", description = "获取文件的预览内容，支持PDF分页、Word转HTML")
    public Result<PreviewResult> getPreviewContent(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            PreviewResult result = filePreviewService.getPreview(fileUuid, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取预览内容失败: {}", e.getMessage(), e);
            return Result.fail("获取预览内容失败: " + e.getMessage());
        }
    }

    /**
     * 获取PDF指定页面
     */
    @GetMapping("/{fileUuid}/preview-page/{pageNumber}")
    @Operation(summary = "获取PDF指定页面", description = "获取PDF文件的指定页面内容")
    public Result<FilePreviewService.PageContent> getPdfPage(
            @PathVariable String fileUuid,
            @PathVariable Integer pageNumber,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FilePreviewService.PageContent page = filePreviewService.getPdfPage(fileUuid, pageNumber, userId);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取PDF页面失败: {}", e.getMessage(), e);
            return Result.fail("获取PDF页面失败: " + e.getMessage());
        }
    }

    /**
     * 重新处理文件（重新索引向量）
     */
    @PostMapping("/{fileUuid}/reprocess")
    @Operation(summary = "重新处理文件", description = "手动触发处理链重新处理文件，主要用于向量索引")
    public Result<Map<String, String>> reprocessFile(
            @PathVariable String fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("重新处理文件: fileUuid={}, userId={}", fileUuid, userId);
        try {
            fileInfoService.reprocessFile(fileUuid, userId);
            return Result.success(Map.of("message", "已触发重新处理"));
        } catch (Exception e) {
            log.error("重新处理失败: {}", e.getMessage(), e);
            return Result.fail("重新处理失败: " + e.getMessage());
        }
    }

    /**
     * 批量重新处理（为所有indexed=false的文件重新索引向量）
     */
    @PostMapping("/batch/reprocess-unindexed")
    @Operation(summary = "批量重新处理未索引文件", description = "为所有indexed=false的文件触发向量索引")
    public Result<Map<String, String>> batchReprocessUnindexed(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("批量重新处理未索引文件: userId={}", userId);
        try {
            int count = fileInfoService.batchReprocessUnindexed(userId);
            return Result.success(Map.of("message", "已触发批量重新处理", "count", String.valueOf(count)));
        } catch (Exception e) {
            log.error("批量重新处理失败: {}", e.getMessage(), e);
            return Result.fail("批量重新处理失败: " + e.getMessage());
        }
    }
}