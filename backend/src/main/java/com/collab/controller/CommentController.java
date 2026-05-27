package com.collab.controller;

import com.collab.common.Result;
import com.collab.dto.CommentDTO;
import com.collab.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc/{docId}/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<?> addComment(@PathVariable Long docId, @RequestBody CommentDTO commentDTO) {
        return commentService.addComment(docId, commentDTO, getCurrentUserId());
    }

    @GetMapping("/list")
    public Result<?> listComments(@PathVariable Long docId) {
        return commentService.listComments(docId);
    }

    @PutMapping("/{commentId}/resolve")
    public Result<?> resolveComment(@PathVariable Long commentId) {
        return commentService.resolveComment(commentId, getCurrentUserId());
    }

    @DeleteMapping("/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId) {
        return commentService.deleteComment(commentId, getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
