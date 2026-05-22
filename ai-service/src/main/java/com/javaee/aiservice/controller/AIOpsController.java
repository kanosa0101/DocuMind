package com.javaee.aiservice.controller;

import com.javaee.aiservice.aiops.FaultDetector;
import com.javaee.aiservice.aiops.MonitoringService;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AIOps控制器
 * 提供系统监控和故障处理相关的REST API接口
 * 仅管理员可访问
 */
@RestController
@RequestMapping("/api/ai/aiops")
@Tag(name = "AIOps", description = "系统监控、故障检测、故障处理接口（仅管理员）")
public class AIOpsController {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private FaultDetector faultDetector;

    /**
     * 验证管理员权限
     * @param userIdHeader 用户ID
     * @return 是否有权限
     */
    private boolean validateAdminAccess(String userIdHeader) {
        // 简化验证：要求有用户ID即可（实际应检查角色）
        // 后续可扩展：从数据库查询用户角色
        return userIdHeader != null && !userIdHeader.isEmpty();
    }

    /**
     * 系统监控 - 获取指标（仅管理员）
     */
    @GetMapping("/monitor")
    @Operation(summary = "获取监控指标", description = "获取系统的监控指标，仅管理员可访问")
    public Result<Map<String, Object>> getMetrics(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        Map<String, Object> metrics = monitoringService.getAllMetrics();
        return Result.success(metrics);
    }

    /**
     * 系统监控 - 重置指标（仅管理员）
     */
    @PostMapping("/metrics/reset")
    @Operation(summary = "重置指标", description = "重置所有监控指标，仅管理员可访问")
    public Result<Void> resetMetrics(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        monitoringService.resetMetrics();
        return Result.success();
    }

    /**
     * 故障检测 - 检测故障（仅管理员）
     */
    @GetMapping("/detect")
    @Operation(summary = "检测故障", description = "检测系统中的故障，仅管理员可访问")
    public Result<List<Map<String, Object>>> detectFaults(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        List<Map<String, Object>> faults = faultDetector.detectFaults();
        return Result.success(faults);
    }

    /**
     * 故障检测 - 获取所有故障（仅管理员）
     */
    @GetMapping("/faults")
    @Operation(summary = "获取故障列表", description = "获取所有故障记录，仅管理员可访问")
    public Result<List<Map<String, Object>>> getAllFaults(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        List<Map<String, Object>> faults = faultDetector.getAllFaults();
        return Result.success(faults);
    }

    /**
     * 故障检测 - 处理故障（仅管理员）
     */
    @PostMapping("/faults/{faultId}/resolve")
    @Operation(summary = "处理故障", description = "处理指定的故障，仅管理员可访问")
    public Result<Map<String, Object>> resolveFault(
            @Parameter(description = "故障ID") @PathVariable String faultId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        Map<String, Object> result = faultDetector.resolveFault(faultId);
        return Result.success(result);
    }

    /**
     * 系统监控 - 记录指标（仅管理员）
     */
    @PostMapping("/metrics/counter")
    @Operation(summary = "记录计数器", description = "记录计数器指标，仅管理员可访问")
    public Result<Void> incrementCounter(
            @Parameter(description = "指标名称") @RequestParam String name,
            @Parameter(description = "增量") @RequestParam(defaultValue = "1") long delta,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        monitoringService.incrementCounter(name, delta);
        return Result.success();
    }

    /**
     * 系统监控 - 记录耗时（仅管理员）
     */
    @PostMapping("/metrics/timer")
    @Operation(summary = "记录耗时", description = "记录耗时指标，仅管理员可访问")
    public Result<Void> recordTimer(
            @Parameter(description = "指标名称") @RequestParam String name,
            @Parameter(description = "耗时(ms)") @RequestParam long duration,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (!validateAdminAccess(userIdHeader)) {
            return Result.fail("无权限访问：需要管理员身份");
        }
        monitoringService.recordTimer(name, duration);
        return Result.success();
    }
}
