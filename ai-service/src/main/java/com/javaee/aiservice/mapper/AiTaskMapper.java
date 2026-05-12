package com.javaee.aiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.aiservice.entity.AiTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI任务Mapper接口
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {
}