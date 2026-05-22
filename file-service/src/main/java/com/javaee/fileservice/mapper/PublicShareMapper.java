package com.javaee.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.fileservice.entity.PublicShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 公开链接分享数据访问接口 (v3.0)
 */
public interface PublicShareMapper extends BaseMapper<PublicShare> {

    /**
     * 根据分享码获取公开分享
     */
    @Select("SELECT * FROM public_share WHERE share_code = #{shareCode} AND status = 'ACTIVE' " +
            "AND (expire_time IS NULL OR expire_time > NOW())")
    PublicShare selectByShareCode(@Param("shareCode") String shareCode);

    /**
     * 根据文件UUID获取公开链接列表
     */
    @Select("SELECT * FROM public_share WHERE file_uuid = #{fileUuid} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<PublicShare> selectByFileUuid(@Param("fileUuid") String fileUuid);

    /**
     * 获取用户创建的公开链接
     */
    @Select("SELECT * FROM public_share WHERE owner_id = #{userId} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<PublicShare> selectByOwner(@Param("userId") Long userId);

    /**
     * 增加下载计数
     */
    @Update("UPDATE public_share SET download_count = download_count + 1, update_time = NOW() WHERE share_code = #{shareCode}")
    int incrementDownloadCount(@Param("shareCode") String shareCode);

    /**
     * 取消公开链接
     */
    @Update("UPDATE public_share SET status = 'CANCELLED', update_time = NOW() WHERE id = #{id}")
    int cancelShare(@Param("id") Long id);

    /**
     * 标记过期链接
     */
    @Update("UPDATE public_share SET status = 'EXPIRED', update_time = NOW() WHERE expire_time < NOW() AND status = 'ACTIVE'")
    int markExpiredShares();

    /**
     * 检查下载限制（返回是否超限）
     */
    @Select("SELECT CASE WHEN download_limit = -1 THEN 0 WHEN download_count >= download_limit THEN 1 ELSE 0 END FROM public_share WHERE share_code = #{shareCode}")
    int checkDownloadLimit(@Param("shareCode") String shareCode);
}