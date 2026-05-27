package com.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long userId;

    private String operationType;

    private String operationData;

    private Integer baseVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
