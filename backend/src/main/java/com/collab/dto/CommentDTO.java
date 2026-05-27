package com.collab.dto;

import lombok.Data;

@Data
public class CommentDTO {

    private String content;

    private Long parentId;

    private String positionInfo;
}
