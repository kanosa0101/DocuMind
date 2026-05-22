package com.javaee.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.fileservice.entity.FileShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文件分享数据访问接口 (v3.0)
 */
public interface FileShareMapper extends BaseMapper<FileShare> {

    /**
     * 根据文件UUID获取分享列表
     */
    @Select("SELECT * FROM file_share WHERE file_uuid = #{fileUuid} AND status = 'ACTIVE'")
    List<FileShare> selectByFileUuid(@Param("fileUuid") String fileUuid);

    /**
     * 获取分享给我的文件列表
     */
    @Select("SELECT * FROM file_share WHERE share_to_id = #{userId} AND status = 'ACTIVE' " +
            "AND (expire_time IS NULL OR expire_time > NOW()) ORDER BY create_time DESC")
    List<FileShare> selectSharedToMe(@Param("userId") Long userId);

    /**
     * 获取我分享出去的文件列表
     */
    @Select("SELECT * FROM file_share WHERE owner_id = #{userId} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<FileShare> selectMyShares(@Param("userId") Long userId);

    /**
     * 检查分享是否存在
     */
    @Select("SELECT * FROM file_share WHERE file_uuid = #{fileUuid} AND share_to_id = #{shareToId} AND status = 'ACTIVE'")
    FileShare selectByFileAndTarget(@Param("fileUuid") String fileUuid, @Param("shareToId") Long shareToId);

    /**
     * 取消分享
     */
    @Update("UPDATE file_share SET status = 'CANCELLED', update_time = NOW() WHERE id = #{id}")
    int cancelShare(@Param("id") Long id);

    /**
     * 标记过期分享
     */
    @Update("UPDATE file_share SET status = 'EXPIRED', update_time = NOW() WHERE expire_time < NOW() AND status = 'ACTIVE'")
    int markExpiredShares();
}