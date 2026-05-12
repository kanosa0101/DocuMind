package com.javaee.fileservice.service;

import com.javaee.fileservice.entity.FileMetadata;
import java.util.List;

/**
 * 文件元数据服务接口
 * 所有查询方法都需要 userId 参数实现用户隔离
 */
public interface FileMetadataService {

    /**
     * 根据文件ID获取元数据
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限验证，可选）
     */
    FileMetadata getMetadata(String fileId, Long userId);

    /**
     * 根据文件ID获取元数据（不验证用户权限，内部使用）
     */
    FileMetadata getMetadata(String fileId);

    /**
     * 保存文件元数据
     */
    void saveMetadata(FileMetadata fileMetadata);

    /**
     * 更新文件元数据
     */
    void updateMetadata(FileMetadata fileMetadata);

    /**
     * 删除文件元数据（软删除）
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限验证）
     */
    void deleteMetadata(String fileId, Long userId);

    /**
     * 删除文件元数据（内部使用）
     */
    void deleteMetadata(String fileId);

    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param direction 排序方向
     */
    List<FileMetadata> getFileList(Long userId, int page, int size, String sortBy, String direction);

    /**
     * 获取用户文件总数
     * @param userId 用户ID
     */
    long getFileCount(Long userId);

    /**
     * 搜索用户文件
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     */
    List<FileMetadata> searchFiles(Long userId, String keyword, int page, int size);

    /**
     * 获取用户搜索结果总数
     * @param userId 用户ID
     * @param keyword 关键词
     */
    long getSearchFileCount(Long userId, String keyword);

    /**
     * 获取目录结构
     */
    Object getDirectoryStructure(String path);

    /**
     * 验证文件是否属于指定用户
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    boolean isFileOwnedByUser(String fileId, Long userId);

} 
