package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.Result;
import com.collab.dto.CommentDTO;
import com.collab.entity.DocumentComment;
import com.collab.entity.User;
import com.collab.mapper.DocumentCommentMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final DocumentCommentMapper documentCommentMapper;
    private final UserMapper userMapper;

    @Override
    public Result<?> addComment(Long docId, CommentDTO dto, Long userId) {
        DocumentComment comment = new DocumentComment();
        comment.setDocumentId(docId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setPositionInfo(dto.getPositionInfo());
        comment.setResolved(0);
        documentCommentMapper.insert(comment);
        return Result.success(comment);
    }

    @Override
    public Result<?> listComments(Long docId) {
        LambdaQueryWrapper<DocumentComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentComment::getDocumentId, docId)
                .orderByAsc(DocumentComment::getCreatedAt);
        List<DocumentComment> comments = documentCommentMapper.selectList(wrapper);

        Set<Long> userIds = comments.stream()
                .map(DocumentComment::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        List<Map<String, Object>> result = comments.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("documentId", c.getDocumentId());
            map.put("userId", c.getUserId());
            map.put("content", c.getContent());
            map.put("parentId", c.getParentId());
            map.put("positionInfo", c.getPositionInfo());
            map.put("resolved", c.getResolved());
            map.put("createdAt", c.getCreatedAt());
            map.put("updatedAt", c.getUpdatedAt());
            User u = userMap.get(c.getUserId());
            if (u != null) {
                map.put("nickname", u.getNickname());
                map.put("avatar", u.getAvatar());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<?> resolveComment(Long commentId, Long userId) {
        DocumentComment comment = documentCommentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        comment.setResolved(1);
        documentCommentMapper.updateById(comment);
        return Result.success();
    }

    @Override
    public Result<?> deleteComment(Long commentId, Long userId) {
        DocumentComment comment = documentCommentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            return Result.fail("只能删除自己的评论");
        }
        documentCommentMapper.deleteById(commentId);
        return Result.success();
    }
}
