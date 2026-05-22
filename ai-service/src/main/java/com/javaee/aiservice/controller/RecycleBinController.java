package com.javaee.aiservice.controller;

import com.javaee.common.model.Result;
import com.javaee.aiservice.service.RecycleBinService;
import com.javaee.aiservice.vo.RecycleBinVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站控制器
 * 提供回收站文件管理接口（用户隔离）
 */
@Slf4j
@RestController
@RequestMapping("/api/files/recycle")
@Tag(name = "回收站管理", description = "回收站文件列表、恢复、永久删除等接口")
public class RecycleBinController {

    @Autowired
    private RecycleBinService recycleBinService;

    /**
     * 获取用户回收站文件列表
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 回收站文件列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取回收站文件列表", description = "获取当前用户删除的文件列表")
    public Result<RecycleBinVO> getRecycleBinList(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            Long userId = parseUserId(userIdHeader);
            RecycleBinVO vo = recycleBinService.listRecycleBinByUserId(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取回收站列表失败: {}", e.getMessage(), e);
            return Result.fail("获取回收站列表失败: " + e.getMessage());
        }
    }

    /**
     * 从回收站恢复文件
     * @param recycleId 回收站记录ID
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 恢复结果
     */
    @PostMapping("/restore/{recycleId}")
    @Operation(summary = "恢复文件", description = "从回收站恢复已删除的文件")
    public Result<String> restoreFile(
            @Parameter(description = "回收站记录ID") @PathVariable String recycleId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            Long userId = parseUserId(userIdHeader);
            // 验证所有权（回收站记录必须属于该用户）
            // TODO: 添加所有权验证

            String objectName = recycleBinService.restoreFromRecycleBin(recycleId, null);
            log.info("文件恢复成功: recycleId={}, userId={}", recycleId, userId);
            return Result.success(objectName);
        } catch (Exception e) {
            log.error("恢复文件失败: {}", e.getMessage(), e);
            return Result.fail("恢复文件失败: " + e.getMessage());
        }
    }

    /**
     * 永久删除回收站中的文件
     * @param recycleId 回收站记录ID
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 删除结果
     */
    @DeleteMapping("/{recycleId}")
    @Operation(summary = "永久删除文件", description = "从回收站永久删除文件，不可恢复")
    public Result<Void> permanentDelete(
            @Parameter(description = "回收站记录ID") @PathVariable String recycleId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            Long userId = parseUserId(userIdHeader);
            // 验证所有权
            // TODO: 添加所有权验证

            recycleBinService.permanentDelete(recycleId);
            log.info("文件永久删除成功: recycleId={}, userId={}", recycleId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("永久删除失败: {}", e.getMessage(), e);
            return Result.fail("永久删除失败: " + e.getMessage());
        }
    }

    /**
     * 解析用户ID
     */
    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new IllegalArgumentException("用户身份验证失败：缺少用户ID");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("用户身份验证失败：用户ID格式错误");
        }
    }
}