package com.javaee.documentservice.controller;

import com.javaee.common.model.Result;
import com.javaee.documentservice.dto.DocumentCreateDTO;
import com.javaee.documentservice.dto.DocumentQueryDTO;
import com.javaee.documentservice.dto.DocumentUpdateDTO;
import com.javaee.documentservice.entity.DocumentVersion;
import com.javaee.documentservice.service.DocumentService;
import com.javaee.documentservice.vo.DocumentVO;
import com.javaee.documentservice.vo.DocumentVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档管理控制器
 * 提供文档的CRUD和版本控制REST API接口
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档管理", description = "文档创建、更新、删除、查询、版本控制等接口")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 创建文档
     * @param dto 创建文档请求参数
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 文档VO
     */
    @PostMapping
    @Operation(summary = "创建文档", description = "创建新文档，自动保存初始版本")
    public Result<DocumentVO> create(
            @RequestBody DocumentCreateDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        DocumentVO document = documentService.create(dto, userId);
        return Result.success(document);
    }

    /**
     * 更新文档
     * @param id 文档ID
     * @param dto 更新文档请求参数
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 更新后的文档VO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新文档", description = "更新文档内容，自动保存历史版本")
    public Result<DocumentVO> update(
            @Parameter(description = "文档ID") @PathVariable String id,
            @RequestBody DocumentUpdateDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        DocumentVO document = documentService.update(id, dto, userId);
        return Result.success(document);
    }

    /**
     * 删除文档
     * @param id 文档ID
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 无
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "软删除文档，将文档状态标记为已删除")
    public Result<Void> delete(
            @Parameter(description = "文档ID") @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        documentService.delete(id, userId);
        return Result.success();
    }

    /**
     * 获取文档详情
     * @param id 文档ID
     * @return 文档VO
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据文档ID获取文档详细信息")
    public Result<DocumentVO> getById(@Parameter(description = "文档ID") @PathVariable String id) {
        DocumentVO document = documentService.getById(id);
        return Result.success(document);
    }

    /**
     * 获取用户文档列表
     * @param userId 用户ID
     * @return 文档VO列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户文档列表", description = "获取指定用户的所有活跃文档列表")
    public Result<List<DocumentVO>> getByUserId(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<DocumentVO> documents = documentService.getByUserId(userId);
        return Result.success(documents);
    }

    /**
     * 搜索文档
     * @param keyword 关键词（可选）
     * @param category 分类（可选）
     * @return 文档VO列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索文档", description = "根据关键词搜索标题、内容、关键词，或按分类筛选文档")
    public Result<List<DocumentVO>> search(
            @Parameter(description = "关键词（搜索标题、内容、关键词）") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false) String category) {
        DocumentQueryDTO dto = new DocumentQueryDTO();
        dto.setKeyword(keyword);
        dto.setCategory(category);
        List<DocumentVO> documents = documentService.search(dto);
        return Result.success(documents);
    }

    /**
     * 获取文档版本列表
     * @param id 文档ID
     * @return 文档版本列表
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "获取文档版本列表", description = "获取文档的所有历史版本，按版本号降序排列")
    public Result<List<DocumentVersionVO>> getVersions(@Parameter(description = "文档ID") @PathVariable String id) {
        List<DocumentVersion> versions = documentService.getVersions(id);
        // 将实体转换为VO，确保字段名与前端一致
        List<DocumentVersionVO> versionVOs = versions.stream()
                .map(this::convertVersionToVO)
                .collect(Collectors.toList());
        return Result.success(versionVOs);
    }

    /**
     * 将DocumentVersion实体转换为DocumentVersionVO
     * 确保字段名与前端接口定义一致
     */
    private DocumentVersionVO convertVersionToVO(DocumentVersion version) {
        DocumentVersionVO vo = new DocumentVersionVO();
        vo.setId(String.valueOf(version.getId()));
        vo.setDocumentId(String.valueOf(version.getDocId()));
        vo.setVersionNumber(version.getVersion());
        vo.setContent(version.getContent());
        vo.setChangeLog(version.getChangeSummary());
        vo.setCreatedBy(String.valueOf(version.getUserId()));
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }

    /**
     * 恢复文档版本
     * @param id 文档ID
     * @param versionNumber 版本号
     * @param userIdHeader 用户ID（从网关JWT解析）
     * @return 恢复后的文档VO
     */
    @PostMapping("/{id}/restore/{versionNumber}")
    @Operation(summary = "恢复文档版本", description = "将文档恢复到指定版本，自动保存当前版本作为历史版本")
    public Result<DocumentVO> restoreVersion(
            @Parameter(description = "文档ID") @PathVariable String id,
            @Parameter(description = "版本号") @PathVariable Integer versionNumber,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        DocumentVO document = documentService.restoreVersion(id, versionNumber, userId);
        return Result.success(document);
    }

    /**
     * 解析用户ID（从网关传递的X-User-Id头）
     * @param userIdHeader 用户ID字符串
     * @return 用户ID（Long类型）
     */
    private Long parseUserId(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                // X-User-Id格式错误，使用默认值
            }
        }
        // 未提供或格式错误时返回默认值（兼容旧逻辑）
        return 1L;
    }
}
