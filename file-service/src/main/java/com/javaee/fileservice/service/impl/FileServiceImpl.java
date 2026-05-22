package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.config.FileStorageConfig;
import com.javaee.fileservice.config.MinioConfig;
import com.javaee.fileservice.service.FileInfoService;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.util.FileUtils;
import com.javaee.fileservice.util.Md5Utils;
import com.javaee.fileservice.util.PathUtils;
import com.javaee.fileservice.entity.FileInfo;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.StatObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private MinioClient minioClient;

    // 用于存储分片上传的临时文件
    private final ConcurrentMap<String, ConcurrentMap<Integer, File>> chunkMap = new ConcurrentHashMap<>();

    // 分片上传锁（防止同一 fileId 的并发上传/合并冲突）
    private final ConcurrentMap<String, Lock> fileLocks = new ConcurrentHashMap<>();

    @Override
    public String upload(MultipartFile file, Long userId) {
        try {
            // 生成文件ID
            String fileId = UUID.randomUUID().toString();
            String fileName = file.getOriginalFilename();
            String fileExtension = FileUtils.getFileExtension(fileName);
            String storageFileName = fileId + (fileExtension != null ? "." + fileExtension : "");

            // 根据存储类型上传文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
            Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
            log.debug("存储路径: {}", storagePath);
            try {
                // 确保存储目录存在
                Path storageDir = storagePath.getParent();
                if (storageDir != null) {
                    if (!Files.exists(storageDir)) {
                        Files.createDirectories(storageDir);
                        log.info("目录创建成功: {}", storageDir);
                    } else {
                        log.debug("目录已存在: {}", storageDir);
                    }
                }

                // 使用FileOutputStream保存文件
                File destFile = storagePath.toFile();
                log.debug("目标文件: {}", destFile.getAbsolutePath());

                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = file.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();
                    log.info("文件上传成功: {}", storagePath);
                }
            } catch (Exception e) {
                log.error("本地存储错误: {}", e.getMessage(), e);
                throw e;
            }
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                log.debug("开始MinIO存储, 桶: {}, 文件: {}, 大小: {}",
                    fileStorageConfig.getBucketName(), storageFileName, file.getSize());
                try {
                    ensureBucketExists(fileStorageConfig.getBucketName());
                    log.debug("存储桶检查/创建成功");
                    try (InputStream inputStream = file.getInputStream()) {
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(storageFileName)
                                        .stream(inputStream, file.getSize(), -1)
                                        .contentType(file.getContentType())
                                        .build()
                        );
                        log.info("文件上传到MinIO成功: {}", storageFileName);
                    }
                } catch (Exception e) {
                    log.error("MinIO存储错误: {}", e.getMessage(), e);
                    throw e;
                }
            }

            // v3.0: 元数据由FileInfoService处理，此处不再操作数据库
            log.info("文件上传成功: fileId={}, userId={}", fileId, userId);

            return fileId;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        return upload(file, null);
    }

    @Override
    public String[] uploadMultiple(MultipartFile[] files, Long userId) {
        String[] fileIds = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileIds[i] = upload(files[i], userId);
        }
        return fileIds;
    }

    @Override
    public void uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks) {
        // 获取该 fileId 的锁，防止并发冲突
        Lock lock = fileLocks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 确保文件ID对应的分片映射存在
            chunkMap.computeIfAbsent(fileId, k -> new ConcurrentHashMap<>());
            ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);

            // 保存分片文件
            File chunkFile = File.createTempFile("chunk_" + fileId + "_", null);
            chunk.transferTo(chunkFile);
            chunks.put(chunkIndex, chunkFile);

            log.debug("分片上传成功: fileId={}, chunkIndex={}, totalChunks={}", fileId, chunkIndex, totalChunks);
        } catch (Exception e) {
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String mergeChunk(String fileId, String fileName, Long userId) {
        // 获取该 fileId 的锁，防止与 uploadChunk 并发冲突
        Lock lock = fileLocks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 获取分片文件
            ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);
            if (chunks == null || chunks.isEmpty()) {
                throw new RuntimeException("没有找到分片文件");
            }

            // 生成存储文件名
            String fileExtension = FileUtils.getFileExtension(fileName);
            String storageFileName = fileId + (fileExtension != null ? "." + fileExtension : "");
            byte[] mergedBytes = null;

            // 根据存储类型合并分片
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储合并
                Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Files.createDirectories(storagePath.getParent());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // 按顺序读取分片并合并
                for (int i = 0; i < chunks.size(); i++) {
                    File chunkFile = chunks.get(i);
                    if (chunkFile != null) {
                        byte[] chunkBytes = Files.readAllBytes(chunkFile.toPath());
                        outputStream.write(chunkBytes);
                        // 删除临时分片文件
                        chunkFile.delete();
                    }
                }

                mergedBytes = outputStream.toByteArray();
                Files.write(storagePath, mergedBytes);
                outputStream.close();
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储合并
                ensureBucketExists(fileStorageConfig.getBucketName());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // 按顺序读取分片并合并
                for (int i = 0; i < chunks.size(); i++) {
                    File chunkFile = chunks.get(i);
                    if (chunkFile != null) {
                        byte[] chunkBytes = Files.readAllBytes(chunkFile.toPath());
                        outputStream.write(chunkBytes);
                        // 删除临时分片文件
                        chunkFile.delete();
                    }
                }

                mergedBytes = outputStream.toByteArray();
                outputStream.close();

                // 上传合并后的文件
                try (InputStream inputStream = FileUtils.toInputStream(mergedBytes)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(fileStorageConfig.getBucketName())
                                    .object(storageFileName)
                                    .stream(inputStream, mergedBytes.length, -1)
                                    .contentType(FileUtils.getContentType(fileName))
                                    .build()
                    );
                }
            }

            // 清理分片映射
            chunkMap.remove(fileId);

            // v3.0: 元数据由FileInfoService处理
            log.info("分片合并成功: fileId={}, userId={}", fileId, userId);

            return fileId;
        } catch (Exception e) {
            // 清理分片文件
            if (chunkMap.containsKey(fileId)) {
                ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);
                chunks.values().forEach(File::delete);
                chunkMap.remove(fileId);
            }
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
            fileLocks.remove(fileId);
        }
    }

    @Override
    public byte[] download(String fileId, Long userId) {
        try {
            // v3.0: 使用FileInfoService验证权限
            if (userId != null) {
                FileInfo fileInfo = fileInfoService.getByUuid(fileId, userId);
                if (fileInfo == null) {
                    throw new RuntimeException("文件不存在或不属于用户: " + fileId);
                }
            }
            return download(fileId);
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String fileId) {
        try {
            // v3.0: 从FileInfo获取存储路径
            FileInfo fileInfo = fileInfoService.getByUuidInternal(fileId);
            String storageFileName = fileInfo != null ? fileInfo.getStoragePath() : fileId;

            // 根据存储类型下载文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                return Files.readAllBytes(storagePath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(fileStorageConfig.getBucketName())
                                .object(storageFileName)
                                .build()
                )) {
                    return FileUtils.toByteArray(inputStream);
                }
            }

            throw new RuntimeException("不支持的存储类型: " + fileStorageConfig.getStorageType());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId, Long userId) {
        try {
            // v3.0: 使用FileInfoService验证权限并软删除
            FileInfo fileInfo = null;
            if (userId != null) {
                fileInfo = fileInfoService.getByUuid(fileId, userId);
                if (fileInfo == null) {
                    throw new RuntimeException("文件不存在或不属于用户: " + fileId);
                }
            } else {
                fileInfo = fileInfoService.getByUuidInternal(fileId);
            }

            String storageFileName = fileInfo != null ? fileInfo.getStoragePath() : fileId;

            // 删除MinIO物理文件
            if (storageFileName != null) {
                if ("minio".equals(fileStorageConfig.getStorageType())) {
                    // v3.0 fix: 使用配置的bucket，而不是从storagePath解析
                    String objectName = storageFileName.contains("/")
                        ? storageFileName.substring(storageFileName.indexOf("/") + 1)
                        : storageFileName;
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(fileStorageConfig.getBucketName())
                                    .object(objectName)
                                    .build()
                    );
                    log.info("成功删除MinIO文件: bucket={}, object={}", fileStorageConfig.getBucketName(), objectName);
                } else if ("local".equals(fileStorageConfig.getStorageType())) {
                    Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                    Files.deleteIfExists(storagePath);
                }
            }

            // v3.0: 执行软删除，更新数据库状态为DELETED
            fileInfoService.softDelete(fileId, userId);
            log.info("文件软删除成功: fileId={}, userId={}", fileId, userId);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) {
        delete(fileId, null);
    }

    @Override
    public void rename(String fileId, String newName, Long userId) {
        // v3.0: 文件名修改由FileInfoService处理
        // 此方法已简化，物理文件名不变
        log.info("rename请求: fileId={}, newName={}", fileId, newName);
    }

    @Override
    public void move(String fileId, String targetPath, Long userId) {
        // v3.0: 文件移动由FileInfoService处理
        // 此方法已简化，保留MinIO操作
        log.info("move请求: fileId={}, targetPath={}", fileId, targetPath);
    }

    @Override
    public String copy(String fileId, String targetPath, Long userId) {
        // v3.0: 文件复制由FileInfoService处理
        log.info("copy请求: fileId={}, targetPath={}", fileId, targetPath);
        return UUID.randomUUID().toString();
    }

    /**
     * 确保MinIO存储桶存在
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

}
