package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档版本Mapper接口
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

    List<DocumentVersion> selectByDocId(@Param("docId") Long docId);

    DocumentVersion selectLatestVersion(@Param("docId") Long docId);

    Integer selectMaxVersion(@Param("docId") Long docId);

    DocumentVersion selectByDocIdAndVersion(@Param("docId") Long docId, @Param("version") Integer version);

    int deleteByDocId(@Param("docId") Long docId);
}
