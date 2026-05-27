package com.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    List<Document> selectUserDocuments(@Param("userId") Long userId);

    List<Document> selectSharedDocuments(@Param("userId") Long userId);

    List<Document> selectOwnDocuments(@Param("userId") Long userId);
}
