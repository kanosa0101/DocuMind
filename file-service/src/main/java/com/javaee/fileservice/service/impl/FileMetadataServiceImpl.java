package com.javaee.fileservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.javaee.fileservice.entity.FileMetadata;
import com.javaee.fileservice.mapper.FileMetadataMapper;
import com.javaee.fileservice.service.FileMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件元数据服务实现类
 * 所有查询方法都添加用户隔离过滤
 */
@Slf4j
@Service
public class FileMetadataServiceImpl implements FileMetadataService {

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Override
    public FileMetadata getMetadata(String fileId, Long userId) {
        try {
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileId);
            wrapper.eq("status", "ACTIVE");
            // 用户隔离：只返回属于该用户的文件
            if (userId != null) {
                wrapper.eq("user_id", userId);
            }
            return fileMetadataMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("获取元数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public FileMetadata getMetadata(String fileId) {
        try {
            return fileMetadataMapper.selectByFileId(fileId);
        } catch (Exception e) {
            log.error("获取元数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void saveMetadata(FileMetadata fileMetadata) {
        try {
            fileMetadata.setCreateTime(LocalDateTime.now());
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadata.setStatus("ACTIVE");
            fileMetadataMapper.insert(fileMetadata);
        } catch (Exception e) {
            log.error("保存元数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void updateMetadata(FileMetadata fileMetadata) {
        try {
            fileMetadata.setUpdateTime(LocalDateTime.now());
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileMetadata.getFileId());
            fileMetadataMapper.update(fileMetadata, wrapper);
        } catch (Exception e) {
            log.error("更新元数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void deleteMetadata(String fileId, Long userId) {
        try {
            // 先验证文件是否属于该用户
            FileMetadata metadata = getMetadata(fileId, userId);
            if (metadata == null) {
                log.warn("删除失败：文件不存在或不属于用户 {}，文件ID {}", userId, fileId);
                return;
            }
            // 软删除：更新状态为DELETED
            metadata.setStatus("DELETED");
            metadata.setUpdateTime(LocalDateTime.now());
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileId);
            wrapper.eq("user_id", userId); // 再次确保用户隔离
            fileMetadataMapper.update(metadata, wrapper);
        } catch (Exception e) {
            log.error("删除元数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void deleteMetadata(String fileId) {
        try {
            // 软删除：更新状态为DELETED（内部使用，不验证用户）
            FileMetadata metadata = getMetadata(fileId);
            if (metadata != null) {
                metadata.setStatus("DELETED");
                metadata.setUpdateTime(LocalDateTime.now());
                QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
                wrapper.eq("file_id", fileId);
                fileMetadataMapper.update(metadata, wrapper);
            }
        } catch (Exception e) {
            log.error("删除元数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<FileMetadata> getFileList(Long userId, int page, int size, String sortBy, String direction) {
        try {
            Page<FileMetadata> pageObj = new Page<>(page, size);
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();

            // 只查询状态为ACTIVE的文件
            wrapper.eq("status", "ACTIVE");

            // 用户隔离：只返回属于该用户的文件
            if (userId != null) {
                wrapper.eq("user_id", userId);
            }

            if (sortBy != null && !sortBy.isEmpty()) {
                String dbColumn = camelToSnake(sortBy);
                if ("asc".equals(direction)) {
                    wrapper.orderByAsc(dbColumn);
                } else {
                    wrapper.orderByDesc(dbColumn);
                }
            } else {
                wrapper.orderByDesc("create_time");
            }

            Page<FileMetadata> result = fileMetadataMapper.selectPage(pageObj, wrapper);
            return result.getRecords();
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Override
    public List<FileMetadata> searchFiles(Long userId, String keyword, int page, int size) {
        try {
            Page<FileMetadata> pageObj = new Page<>(page, size);
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();

            // 只查询状态为ACTIVE的文件
            wrapper.eq("status", "ACTIVE");

            // 用户隔离：只搜索属于该用户的文件
            if (userId != null) {
                wrapper.eq("user_id", userId);
            }

            // 使用and嵌套来确保状态过滤和关键词搜索同时生效
            wrapper.and(w -> w.like("file_name", keyword).or().like("original_file_name", keyword));
            wrapper.orderByDesc("create_time");

            Page<FileMetadata> result = fileMetadataMapper.selectPage(pageObj, wrapper);
            return result.getRecords();
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public long getFileCount(Long userId) {
        try {
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("status", "ACTIVE");
            // 用户隔离：只统计属于该用户的文件
            if (userId != null) {
                wrapper.eq("user_id", userId);
            }
            return fileMetadataMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("获取文件总数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long getSearchFileCount(Long userId, String keyword) {
        try {
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("status", "ACTIVE");
            // 用户隔离：只统计属于该用户的文件
            if (userId != null) {
                wrapper.eq("user_id", userId);
            }
            wrapper.and(w -> w.like("file_name", keyword).or().like("original_file_name", keyword));
            return fileMetadataMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("获取搜索文件总数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public Object getDirectoryStructure(String path) {
        try {
            // 简单实现目录结构
            Map<String, Object> structure = new HashMap<>();
            List<Map<String, Object>> files = new ArrayList<>();
            List<Map<String, Object>> directories = new ArrayList<>();

            // 这里可以根据实际存储情况实现目录结构的获取
            // 目前返回一个示例结构
            Map<String, Object> dir1 = new HashMap<>();
            dir1.put("name", "documents");
            dir1.put("type", "directory");
            dir1.put("path", path + (path.endsWith("/") ? "" : "/") + "documents");
            directories.add(dir1);

            Map<String, Object> dir2 = new HashMap<>();
            dir2.put("name", "images");
            dir2.put("type", "directory");
            dir2.put("path", path + (path.endsWith("/") ? "" : "/") + "images");
            directories.add(dir2);

            structure.put("directories", directories);
            structure.put("files", files);
            structure.put("currentPath", path);

            return structure;
        } catch (Exception e) {
            log.error("获取目录结构失败: {}", e.getMessage(), e);
            // 返回空的目录结构
            Map<String, Object> structure = new HashMap<>();
            structure.put("directories", new ArrayList<>());
            structure.put("files", new ArrayList<>());
            structure.put("currentPath", path);
            return structure;
        }
    }

    @Override
    public boolean isFileOwnedByUser(String fileId, Long userId) {
        try {
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileId);
            wrapper.eq("status", "ACTIVE");
            wrapper.eq("user_id", userId);
            return fileMetadataMapper.selectCount(wrapper) > 0;
        } catch (Exception e) {
            log.error("验证文件归属失败: {}", e.getMessage(), e);
            return false;
        }
    }

}