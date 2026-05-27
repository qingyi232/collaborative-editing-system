package com.collab.service;

import com.collab.common.Result;
import com.collab.dto.CommentDTO;

public interface CommentService {

    Result<?> addComment(Long docId, CommentDTO dto, Long userId);

    Result<?> listComments(Long docId);

    Result<?> resolveComment(Long commentId, Long userId);

    Result<?> deleteComment(Long commentId, Long userId);
}
