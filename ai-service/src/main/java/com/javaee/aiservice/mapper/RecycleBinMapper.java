package com.javaee.aiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.aiservice.entity.RecycleBin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 回收站Mapper接口
 */
@Mapper
public interface RecycleBinMapper extends BaseMapper<RecycleBin> {

    /**
     * 根据用户ID查询回收站文件列表
     * @param userId 用户ID
     * @return 回收站文件列表
     */
    List<RecycleBin> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询过期的回收站文件
     * @return 过期文件列表
     */
    List<RecycleBin> selectExpired();

    /**
     * 根据回收站记录ID查询
     * @param recycleId 回收站记录ID
     * @return 回收站记录
     */
    RecycleBin selectByRecycleId(@Param("recycleId") String recycleId);

    /**
     * 更新回收站记录状态
     * @param recycleId 回收站记录ID
     * @param status 新状态
     * @return 更新数量
     */
    int updateStatus(@Param("recycleId") String recycleId, @Param("status") String status);

    /**
     * 根据存储桶名称查询回收站文件
     * @param bucketName 存储桶名称
     * @return 回收站文件列表
     */
    List<RecycleBin> selectByBucketName(@Param("bucketName") String bucketName);

    /**
     * 删除过期的回收站记录
     * @return 删除数量
     */
    int deleteExpired();
}