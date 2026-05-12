package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.config.FileStorageConfig;
import com.javaee.fileservice.config.MinioConfig;
import com.javaee.fileservice.service.FileMetadataService;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.util.FileUtils;
import com.javaee.fileservice.util.Md5Utils;
import com.javaee.fileservice.util.PathUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.StatObjectArgs;
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

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private MinioClient minioClient;

    // 用于存储分片上传的临时文件
    private final ConcurrentMap<String, ConcurrentMap<Integer, File>> chunkMap = new ConcurrentHashMap<>();

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
            System.out.println("存储路径: " + storagePath);
            try {
                // 确保存储目录存在
                Path storageDir = storagePath.getParent();
                if (storageDir != null) {
                    if (!Files.exists(storageDir)) {
                        Files.createDirectories(storageDir);
                        System.out.println("目录创建成功: " + storageDir);
                    } else {
                        System.out.println("目录已存在: " + storageDir);
                    }
                }

                // 使用FileOutputStream保存文件
                File destFile = storagePath.toFile();
                System.out.println("目标文件: " + destFile.getAbsolutePath());
                System.out.println("目标文件是否存在: " + destFile.exists());
                System.out.println("目标文件是否可写: " + destFile.canWrite());

                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = file.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();
                    System.out.println("文件上传成功: " + storagePath);
                }
            } catch (Exception e) {
                System.out.println("本地存储错误: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                System.out.println("=== 开始MinIO存储 ===");
                System.out.println("存储桶名称: " + fileStorageConfig.getBucketName());
                System.out.println("存储文件名: " + storageFileName);
                System.out.println("文件大小: " + file.getSize());
                System.out.println("文件类型: " + file.getContentType());
                try {
                    ensureBucketExists(fileStorageConfig.getBucketName());
                    System.out.println("存储桶检查/创建成功");
                    try (InputStream inputStream = file.getInputStream()) {
                        System.out.println("获取文件输入流成功");
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(storageFileName)
                                        .stream(inputStream, file.getSize(), -1)
                                        .contentType(file.getContentType())
                                        .build()
                        );
                        System.out.println("文件上传到MinIO成功");
                    }
                } catch (Exception e) {
                    System.out.println("MinIO存储错误: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            // 保存文件元数据（包含用户ID实现隔离）
            try {
                com.javaee.fileservice.entity.FileMetadata fileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                fileMetadata.setFileId(fileId);
                fileMetadata.setFileName(fileName);
                fileMetadata.setOriginalFileName(fileName);
                fileMetadata.setFilePath(fileStorageConfig.getLocalPath());
                fileMetadata.setFileType(file.getContentType());
                fileMetadata.setFileSize(file.getSize());
                fileMetadata.setStorageType(fileStorageConfig.getStorageType());
                fileMetadata.setBucketName(fileStorageConfig.getBucketName());
                fileMetadata.setObjectKey(storageFileName);
                fileMetadata.setCreateBy("system");
                fileMetadata.setUserId(userId); // 设置用户ID实现隔离
                fileMetadataService.saveMetadata(fileMetadata);
            } catch (Exception e) {
                // 数据库不可用时，继续执行，只记录日志
                System.out.println("数据库不可用，跳过元数据保存: " + e.getMessage());
            }

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
        try {
            // 确保文件ID对应的分片映射存在
            chunkMap.computeIfAbsent(fileId, k -> new ConcurrentHashMap<>());
            ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);

            // 保存分片文件
            File chunkFile = File.createTempFile("chunk_" + fileId + "_", null);
            chunk.transferTo(chunkFile);
            chunks.put(chunkIndex, chunkFile);
        } catch (Exception e) {
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String mergeChunk(String fileId, String fileName, Long userId) {
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

            // 保存文件元数据（包含用户ID）
            try {
                com.javaee.fileservice.entity.FileMetadata fileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                fileMetadata.setFileId(fileId);
                fileMetadata.setFileName(fileName);
                fileMetadata.setOriginalFileName(fileName);
                fileMetadata.setFilePath("minio:" + fileStorageConfig.getBucketName());
                fileMetadata.setFileType(com.javaee.fileservice.util.FileUtils.getContentType(fileName));
                fileMetadata.setFileSize(mergedBytes != null ? mergedBytes.length : 0);
                fileMetadata.setStorageType(fileStorageConfig.getStorageType());
                fileMetadata.setBucketName(fileStorageConfig.getBucketName());
                fileMetadata.setObjectKey(storageFileName);
                fileMetadata.setCreateBy("system");
                fileMetadata.setUserId(userId); // 设置用户ID实现隔离
                fileMetadataService.saveMetadata(fileMetadata);
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("数据库不可用，跳过元数据保存: " + e.getMessage());
            }

            return fileId;
        } catch (Exception e) {
            // 清理分片文件
            if (chunkMap.containsKey(fileId)) {
                ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);
                chunks.values().forEach(File::delete);
                chunkMap.remove(fileId);
            }
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String fileId, Long userId) {
        try {
            // 验证文件是否属于该用户
            if (userId != null) {
                com.javaee.fileservice.entity.FileMetadata metadata = fileMetadataService.getMetadata(fileId, userId);
                if (metadata == null) {
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
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;

            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                }
            } catch (Exception e) {
                // 数据库不可用时，尝试不同的文件扩展名
                System.out.println("数据库不可用，尝试不同的文件扩展名: " + e.getMessage());
                // 尝试常见的文件扩展名
                String[] extensions = {"", ".docx", ".pdf", ".txt", ".jpg", ".png", ".jpeg"};
                for (String ext : extensions) {
                    try {
                        String tempFileName = fileId + ext;
                        try (InputStream inputStream = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(tempFileName)
                                        .build()
                        )) {
                            return FileUtils.toByteArray(inputStream);
                        }
                    } catch (Exception ex) {
                        // 忽略错误，尝试下一个扩展名
                        System.out.println("尝试扩展名失败: " + ext);
                    }
                }
                // 如果所有扩展名都失败，抛出异常
                throw new RuntimeException("文件不存在: " + fileId);
            }

            // 如果还是没有storageFileName，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

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
            // 验证文件是否属于该用户（用户隔离）
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            if (userId != null) {
                fileMetadata = fileMetadataService.getMetadata(fileId, userId);
                if (fileMetadata == null) {
                    throw new RuntimeException("文件不存在或不属于用户: " + fileId);
                }
            } else {
                fileMetadata = fileMetadataService.getMetadata(fileId);
            }

            String storageFileName = fileMetadata != null ? fileMetadata.getObjectKey() : null;

            // 删除物理文件
            if (storageFileName != null) {
                if ("minio".equals(fileStorageConfig.getStorageType())) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(fileStorageConfig.getBucketName())
                                    .object(storageFileName)
                                    .build()
                    );
                    System.out.println("成功删除MinIO文件: " + storageFileName);
                } else if ("local".equals(fileStorageConfig.getStorageType())) {
                    Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                    Files.deleteIfExists(storagePath);
                }
            }

            // 删除文件元数据（带用户验证）
            fileMetadataService.deleteMetadata(fileId, userId);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void rename(String fileId, String newName, Long userId) {
        try {
            // 验证文件是否属于该用户（用户隔离）
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            if (userId != null) {
                fileMetadata = fileMetadataService.getMetadata(fileId, userId);
                if (fileMetadata == null) {
                    throw new RuntimeException("文件不存在或不属于用户: " + fileId);
                }
            } else {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata == null) {
                    throw new RuntimeException("文件不存在: " + fileId);
                }
            }

            // 只更新数据库中的显示文件名，不改变MinIO中的objectKey
            // 这样可以保持文件在存储中的位置不变

            // 更新文件元数据
            try {
                fileMetadata.setFileName(newName);
                fileMetadata.setOriginalFileName(newName);
                // 不更新objectKey，保持存储位置不变
                fileMetadataService.updateMetadata(fileMetadata);
                System.out.println("文件元数据重命名成功: " + fileId + " -> " + newName);
            } catch (Exception e) {
                throw new RuntimeException("更新文件元数据失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件重命名失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void move(String fileId, String targetPath) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                    System.out.println("从数据库获取到文件元数据，存储文件名: " + storageFileName);
                } else {
                    System.out.println("数据库中未找到文件元数据: " + fileId);
                }
            } catch (Exception e) {
                // 数据库不可用时，使用fileId作为默认值
                System.out.println("数据库不可用: " + e.getMessage());
                storageFileName = fileId;
            }

            // 如果没有存储文件名，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

            // 根据存储类型移动文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path oldPath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Path newPath = Paths.get(targetPath, storageFileName);
                Files.createDirectories(newPath.getParent());
                Files.move(oldPath, newPath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储（先复制再删除）
                try {
                    // 尝试不同的文件扩展名找到原文件
                    String[] extensions = {"", ".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg"};
                    String foundStorageFileName = null;
                    
                    for (String ext : extensions) {
                        try {
                            String tempFileName = storageFileName + ext;
                            // 检查文件是否存在
                            minioClient.statObject(
                                    StatObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempFileName)
                                            .build()
                            );
                            foundStorageFileName = tempFileName;
                            break;
                        } catch (Exception ex) {
                            // 忽略错误，尝试下一个扩展名
                        }
                    }
                    
                    if (foundStorageFileName != null) {
                        // 构建新的存储路径
                        String newStoragePath = targetPath + "/" + foundStorageFileName;
                        // 移除开头的斜杠
                        if (newStoragePath.startsWith("/")) {
                            newStoragePath = newStoragePath.substring(1);
                        }
                        
                        // 复制文件
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(newStoragePath)
                                        .source(
                                                CopySource.builder()
                                                        .bucket(fileStorageConfig.getBucketName())
                                                        .object(foundStorageFileName)
                                                        .build()
                                        )
                                        .build()
                        );
                        // 删除原文件
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(foundStorageFileName)
                                        .build()
                        );
                        System.out.println("MinIO文件移动成功: " + foundStorageFileName + " -> " + newStoragePath);
                    } else {
                        throw new RuntimeException("文件不存在: " + storageFileName);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("MinIO文件移动失败: " + e.getMessage(), e);
                }
            }

            // 更新文件元数据（如果数据库可用）
            try {
                if (fileMetadata != null) {
                    fileMetadata.setFilePath(targetPath);
                    fileMetadataService.updateMetadata(fileMetadata);
                }
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("更新文件元数据失败: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("文件移动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String copy(String fileId, String targetPath) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            String fileName = "copy_" + fileId + ".txt";
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                    fileName = fileMetadata.getFileName();
                    System.out.println("从数据库获取到文件元数据，存储文件名: " + storageFileName);
                } else {
                    System.out.println("数据库中未找到文件元数据: " + fileId);
                }
            } catch (Exception e) {
                // 数据库不可用时，使用fileId作为默认值
                System.out.println("数据库不可用: " + e.getMessage());
                storageFileName = fileId;
            }

            // 如果没有存储文件名，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

            // 生成新的文件ID
            String newFileId = UUID.randomUUID().toString();
            String newFileExtension = FileUtils.getFileExtension(fileName);
            String newStorageFileName = newFileId + (newFileExtension != null ? "." + newFileExtension : ".txt");

            // 根据存储类型复制文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path oldPath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Path newPath = Paths.get(targetPath, newStorageFileName);
                Files.createDirectories(newPath.getParent());
                Files.copy(oldPath, newPath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                try {
                    // 尝试不同的文件扩展名找到原文件
                    String[] extensions = {"", ".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg"};
                    String foundStorageFileName = null;
                    
                    for (String ext : extensions) {
                        try {
                            String tempFileName = storageFileName + ext;
                            // 检查文件是否存在
                            minioClient.statObject(
                                    StatObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempFileName)
                                            .build()
                            );
                            foundStorageFileName = tempFileName;
                            break;
                        } catch (Exception ex) {
                            // 忽略错误，尝试下一个扩展名
                        }
                    }
                    
                    if (foundStorageFileName != null) {
                        // 构建新的存储路径
                        String newStoragePath = targetPath + "/" + newStorageFileName;
                        // 移除开头的斜杠
                        if (newStoragePath.startsWith("/")) {
                            newStoragePath = newStoragePath.substring(1);
                        }
                        
                        // 复制文件
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(newStoragePath)
                                        .source(
                                                CopySource.builder()
                                                        .bucket(fileStorageConfig.getBucketName())
                                                        .object(foundStorageFileName)
                                                        .build()
                                        )
                                        .build()
                        );
                        System.out.println("MinIO文件复制成功: " + foundStorageFileName + " -> " + newStoragePath);
                    } else {
                        throw new RuntimeException("文件不存在: " + storageFileName);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("MinIO文件复制失败: " + e.getMessage(), e);
                }
            }

            // 保存新文件的元数据（如果数据库可用）
            try {
                if (fileMetadata != null) {
                    com.javaee.fileservice.entity.FileMetadata newFileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                    newFileMetadata.setFileId(newFileId);
                    newFileMetadata.setFileName(fileName);
                    newFileMetadata.setOriginalFileName(fileName);
                    newFileMetadata.setFilePath(targetPath);
                    newFileMetadata.setFileType(fileMetadata.getFileType());
                    newFileMetadata.setFileSize(fileMetadata.getFileSize());
                    newFileMetadata.setStorageType(fileMetadata.getStorageType());
                    newFileMetadata.setBucketName(fileMetadata.getBucketName());
                    newFileMetadata.setObjectKey(newStorageFileName);
                    newFileMetadata.setCreateBy("system");
                    fileMetadataService.saveMetadata(newFileMetadata);
                }
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("保存新文件元数据失败: " + e.getMessage());
            }

            return newFileId;
        } catch (Exception e) {
            throw new RuntimeException("文件复制失败: " + e.getMessage(), e);
        }
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
