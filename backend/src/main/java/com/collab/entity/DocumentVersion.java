package com.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_version")
public class DocumentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer versionNumber;

    private String content;

    private String contentHash;

    private Long operatorId;

    private String changeSummary;

    private Long docSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
