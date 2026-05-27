package com.collab.dto;

import lombok.Data;

@Data
public class DocumentDTO {

    private String title;

    private String content;

    private Boolean isPublic;
}
