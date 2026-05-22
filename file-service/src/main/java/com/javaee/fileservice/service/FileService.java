package com.javaee.fileservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 单文件上传
     * @param file 文件
     * @param userId 用户ID（用于用户隔离）
     */
    String upload(MultipartFile file, Long userId);

    /**
     * 单文件上传（无用户ID，内部使用）
     */
    String upload(MultipartFile file);

    /**
     * 多文件上传
     */
    String[] uploadMultiple(MultipartFile[] files, Long userId);

    /**
     * 分片上传
     */
    void uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks);

    /**
     * 分片合并
     */
    String mergeChunk(String fileId, String fileName, Long userId);

    /**
     * 文件下载（需验证用户权限）
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限验证）
     */
    byte[] download(String fileId, Long userId);

    /**
     * 文件下载（内部使用，不验证权限）
     */
    byte[] download(String fileId);

    /**
     * 文件删除（需验证用户权限）
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限验证）
     */
    void delete(String fileId, Long userId);

    /**
     * 文件删除（内部使用）
     */
    void delete(String fileId);

    /**
     * 文件重命名（需验证用户权限）
     */
    void rename(String fileId, String newName, Long userId);

    /**
     * 文件移动（需验证用户权限）
     * @param fileId 文件ID
     * @param targetPath 目标目录
     * @param userId 用户ID（用于权限验证）
     */
    void move(String fileId, String targetPath, Long userId);

    /**
     * 文件复制（需验证用户权限，新文件归属于当前用户）
     * @param fileId 文件ID
     * @param targetPath 目标目录
     * @param userId 用户ID（用于权限验证和设置新文件归属）
     * @return 新文件ID
     */
    String copy(String fileId, String targetPath, Long userId);

}
