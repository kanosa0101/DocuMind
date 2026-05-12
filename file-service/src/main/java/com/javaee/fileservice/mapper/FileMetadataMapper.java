package com.javaee.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.fileservice.entity.FileMetadata;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件元数据数据访问接口
 */
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 根据文件ID获取文件元数据
     */
    @Select("SELECT * FROM file_metadata WHERE file_id = #{fileId}")
    FileMetadata selectByFileId(@Param("fileId") String fileId);

    /**
     * 根据文件名搜索文件
     */
    @Select("SELECT * FROM file_metadata WHERE file_name LIKE CONCAT('%', #{keyword}, '%') OR original_file_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<FileMetadata> searchByFileName(@Param("keyword") String keyword);

}
