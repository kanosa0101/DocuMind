package com.javaee.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.fileservice.entity.FileInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 文件信息数据访问接口 (v3.0)
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 根据文件UUID获取文件信息
     */
    @Select("SELECT * FROM file_info WHERE file_uuid = #{fileUuid}")
    FileInfo selectByFileUuid(@Param("fileUuid") String fileUuid);

    /**
     * 根据用户ID获取文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<FileInfo> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取已删除文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND status = 'DELETED' ORDER BY delete_time DESC")
    List<FileInfo> selectDeletedByUserId(@Param("userId") Long userId);

    /**
     * 根据分类获取文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND category = #{category} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<FileInfo> selectByCategory(@Param("userId") Long userId, @Param("category") String category);

    /**
     * 搜索文件（文件名、摘要）
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND status = 'ACTIVE' AND " +
            "(file_name LIKE CONCAT('%', #{keyword}, '%') OR original_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR summary LIKE CONCAT('%', #{keyword}, '%')) ORDER BY create_time DESC")
    List<FileInfo> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 获取用户的文件统计
     */
    @Select("SELECT category, COUNT(*) as count FROM file_info WHERE user_id = #{userId} AND status = 'ACTIVE' GROUP BY category")
    List<java.util.Map<String, Object>> selectCategoryStats(@Param("userId") Long userId);

    /**
     * 软删除文件
     */
    @Update("UPDATE file_info SET status = 'DELETED', delete_time = NOW() WHERE file_uuid = #{fileUuid} AND user_id = #{userId}")
    int softDelete(@Param("fileUuid") String fileUuid, @Param("userId") Long userId);

    /**
     * 恢复文件
     */
    @Update("UPDATE file_info SET status = 'ACTIVE', delete_time = NULL WHERE file_uuid = #{fileUuid} AND user_id = #{userId}")
    int restore(@Param("fileUuid") String fileUuid, @Param("userId") Long userId);

    /**
     * 更新版本历史JSON
     */
    @Update("UPDATE file_info SET version_history = #{versionHistory}, version = #{version}, update_time = NOW() WHERE file_uuid = #{fileUuid}")
    int updateVersionHistory(@Param("fileUuid") String fileUuid, @Param("versionHistory") String versionHistory, @Param("version") Integer version);

    /**
     * 更新AI分析结果
     */
    @Update("UPDATE file_info SET summary = #{summary}, keywords = #{keywords}, category = #{category}, " +
            "process_status = 'COMPLETED', process_time = NOW(), update_time = NOW() WHERE file_uuid = #{fileUuid}")
    int updateAiResult(@Param("fileUuid") String fileUuid, @Param("summary") String summary,
                       @Param("keywords") String keywords, @Param("category") String category);

    /**
     * 更新分类
     */
    @Update("UPDATE file_info SET category = #{category}, update_time = NOW() WHERE file_uuid = #{fileUuid} AND user_id = #{userId}")
    int updateCategory(@Param("fileUuid") String fileUuid, @Param("userId") Long userId, @Param("category") String category);

    /**
     * 更新处理状态
     */
    @Update("UPDATE file_info SET process_status = #{processStatus}, update_time = NOW() WHERE file_uuid = #{fileUuid}")
    int updateProcessStatus(@Param("fileUuid") String fileUuid, @Param("processStatus") String processStatus);

    /**
     * 更新向量索引信息
     */
    @Update("UPDATE file_info SET indexed = #{indexed}, vector_id = #{vectorId}, index_time = NOW(), update_time = NOW() WHERE file_uuid = #{fileUuid}")
    int updateVectorInfo(@Param("fileUuid") String fileUuid, @Param("indexed") Boolean indexed, @Param("vectorId") String vectorId);

    /**
     * 标记向量删除
     */
    @Update("UPDATE file_info SET indexed = false, update_time = NOW() WHERE file_uuid = #{fileUuid}")
    int markVectorDeleted(@Param("fileUuid") String fileUuid);

    /**
     * 获取待处理文件
     */
    @Select("SELECT * FROM file_info WHERE process_status = 'PENDING' ORDER BY create_time ASC LIMIT #{limit}")
    List<FileInfo> selectPendingFiles(@Param("limit") int limit);

    /**
     * 检查文件UUID是否存在
     */
    @Select("SELECT COUNT(*) FROM file_info WHERE file_uuid = #{fileUuid}")
    int countByFileUuid(@Param("fileUuid") String fileUuid);

    /**
     * 查询指定时间之前软删除的文件（用于定时清理）
     */
    @Select("SELECT * FROM file_info WHERE status = 'DELETED' AND delete_time < #{threshold}")
    List<FileInfo> findDeletedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 获取用户未索引的文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND status = 'ACTIVE' AND indexed = false")
    List<FileInfo> selectUnindexedByUserId(@Param("userId") Long userId);
}