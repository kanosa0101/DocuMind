package com.javaee.fileservice.controller;

import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.javaee.fileservice.service.FileMetadataService;
import com.javaee.fileservice.entity.FileMetadata;

/**
 * 文件元数据查询接口控制器
 * 所有接口都需要 X-User-Id 请求头，实现用户文件隔离
 */
@Slf4j
@RestController
@RequestMapping("/api/files/metadata")
@Tag(name = "文件元数据", description = "文件元数据查询接口")
public class FileMetadataController {

    @Autowired
    private FileMetadataService fileMetadataService;

    /**
     * 获取文件元数据（用户隔离）
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "获取文件元数据", description = "根据文件ID获取当前用户拥有的文件详细信息")
    public Result<FileMetadata> getMetadata(@Parameter(description = "文件ID") @PathVariable String fileId,
                                            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
            if (metadata == null) {
                return Result.fail("文件不存在或无权访问");
            }
            return Result.success(metadata);
        } catch (Exception e) {
            log.error("获取文件元数据失败: {}", e.getMessage(), e);
            return Result.fail("获取文件元数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件名（用户隔离）
     */
    @GetMapping("/{fileId}/name")
    @Operation(summary = "获取文件名", description = "根据文件ID获取当前用户拥有的文件名")
    public Result<String> getFileName(@Parameter(description = "文件ID") @PathVariable String fileId,
                                      @RequestHeader("X-User-Id") Long userId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
            if (metadata == null) {
                return Result.fail("文件不存在或无权访问");
            }
            return Result.success(metadata.getFileName());
        } catch (Exception e) {
            log.error("获取文件名失败: {}", e.getMessage(), e);
            return Result.fail("获取文件名失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件类型（用户隔离）
     */
    @GetMapping("/{fileId}/type")
    @Operation(summary = "获取文件类型", description = "根据文件ID获取当前用户拥有的文件类型")
    public Result<String> getFileType(@Parameter(description = "文件ID") @PathVariable String fileId,
                                      @RequestHeader("X-User-Id") Long userId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
            if (metadata == null) {
                return Result.fail("文件不存在或无权访问");
            }
            return Result.success(metadata.getFileType());
        } catch (Exception e) {
            log.error("获取文件类型失败: {}", e.getMessage(), e);
            return Result.fail("获取文件类型失败: " + e.getMessage());
        }
    }

    /**
     * 获取目录结构
     */
    @GetMapping("/directory")
    @Operation(summary = "获取目录结构", description = "获取文件系统目录结构")
    public Result<Object> getDirectoryStructure(@Parameter(description = "目录路径") @RequestParam(defaultValue = "/") String path) {
        try {
            Object directoryStructure = fileMetadataService.getDirectoryStructure(path);
            return Result.success(directoryStructure);
        } catch (Exception e) {
            log.error("获取目录结构失败: {}", e.getMessage(), e);
            return Result.fail("获取目录结构失败: " + e.getMessage());
        }
    }
}