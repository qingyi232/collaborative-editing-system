package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.Result;
import com.collab.dto.MemberDTO;
import com.collab.entity.Document;
import com.collab.entity.DocumentMember;
import com.collab.entity.User;
import com.collab.mapper.DocumentMapper;
import com.collab.mapper.DocumentMemberMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.AuditLogService;
import com.collab.service.DocumentMemberService;
import com.collab.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentMemberServiceImpl implements DocumentMemberService {

    private final DocumentMemberMapper documentMemberMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    @Override
    public Result<?> addMember(Long docId, MemberDTO dto, Long operatorId) {
        if (!hasManagePermission(docId, operatorId)) {
            return Result.fail("无权管理文档成员");
        }

        User targetUser = userMapper.selectById(dto.getUserId());
        if (targetUser == null) {
            return Result.fail("用户不存在");
        }

        String maxCollabStr = systemConfigService.getConfigValue("max_collaborators");
        if (maxCollabStr != null) {
            try {
                int maxCollaborators = Integer.parseInt(maxCollabStr);
                LambdaQueryWrapper<DocumentMember> countWrapper = new LambdaQueryWrapper<>();
                countWrapper.eq(DocumentMember::getDocumentId, docId);
                long currentCount = documentMemberMapper.selectCount(countWrapper);
                if (currentCount >= maxCollaborators) {
                    return Result.fail("文档协作人数已达上限（最大 " + maxCollaborators + " 人）");
                }
            } catch (NumberFormatException ignored) {}
        }

        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId)
                .eq(DocumentMember::getUserId, dto.getUserId());
        if (documentMemberMapper.selectCount(wrapper) > 0) {
            return Result.fail("该用户已是文档成员");
        }

        DocumentMember member = new DocumentMember();
        member.setDocumentId(docId);
        member.setUserId(dto.getUserId());
        member.setPermission(dto.getPermission() != null ? dto.getPermission() : "VIEW");
        member.setInvitedBy(operatorId);
        documentMemberMapper.insert(member);

        String operatorName = getUsername(operatorId);
        auditLogService.log(operatorId, operatorName, "ADD_MEMBER", "DOCUMENT", docId,
                "添加成员 " + targetUser.getUsername() + "，权限: " + member.getPermission(), null);

        return Result.success();
    }

    @Override
    public Result<?> removeMember(Long docId, Long userId, Long operatorId) {
        if (!hasManagePermission(docId, operatorId)) {
            return Result.fail("无权管理文档成员");
        }

        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId)
                .eq(DocumentMember::getUserId, userId);
        documentMemberMapper.delete(wrapper);

        String operatorName = getUsername(operatorId);
        User removedUser = userMapper.selectById(userId);
        String removedName = removedUser != null ? removedUser.getUsername() : String.valueOf(userId);
        auditLogService.log(operatorId, operatorName, "REMOVE_MEMBER", "DOCUMENT", docId,
                "移除成员 " + removedName, null);

        return Result.success();
    }

    @Override
    public Result<?> updatePermission(Long docId, MemberDTO dto, Long operatorId) {
        if (!hasManagePermission(docId, operatorId)) {
            return Result.fail("无权管理文档成员");
        }

        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId)
                .eq(DocumentMember::getUserId, dto.getUserId());
        DocumentMember member = documentMemberMapper.selectOne(wrapper);
        if (member == null) {
            return Result.fail("该用户不是文档成员");
        }

        String oldPermission = member.getPermission();
        member.setPermission(dto.getPermission());
        documentMemberMapper.updateById(member);

        String operatorName = getUsername(operatorId);
        User targetUser = userMapper.selectById(dto.getUserId());
        String targetName = targetUser != null ? targetUser.getUsername() : String.valueOf(dto.getUserId());
        auditLogService.log(operatorId, operatorName, "UPDATE_PERMISSION", "DOCUMENT", docId,
                "修改成员 " + targetName + " 权限: " + oldPermission + " → " + dto.getPermission(), null);

        return Result.success();
    }

    @Override
    public Result<?> listMembers(Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            return Result.fail("文档不存在");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        User owner = userMapper.selectById(doc.getOwnerId());
        if (owner != null) {
            Map<String, Object> ownerMap = new LinkedHashMap<>();
            ownerMap.put("userId", owner.getId());
            ownerMap.put("username", owner.getUsername());
            ownerMap.put("nickname", owner.getNickname());
            ownerMap.put("avatar", owner.getAvatar());
            ownerMap.put("permission", "OWNER");
            result.add(ownerMap);
        }

        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId);
        List<DocumentMember> members = documentMemberMapper.selectList(wrapper);

        if (!members.isEmpty()) {
            Set<Long> userIds = members.stream().map(DocumentMember::getUserId).collect(Collectors.toSet());
            List<User> users = userMapper.selectBatchIds(userIds);
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

            for (DocumentMember m : members) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("userId", m.getUserId());
                map.put("permission", m.getPermission());
                map.put("createdAt", m.getCreatedAt());
                User u = userMap.get(m.getUserId());
                if (u != null) {
                    map.put("username", u.getUsername());
                    map.put("nickname", u.getNickname());
                    map.put("avatar", u.getAvatar());
                }
                result.add(map);
            }
        }

        return Result.success(result);
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }

    private boolean hasManagePermission(Long docId, Long operatorId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return false;
        }
        if (doc.getOwnerId().equals(operatorId)) {
            return true;
        }
        User operator = userMapper.selectById(operatorId);
        if (operator != null && ("DOC_ADMIN".equals(operator.getRole()) || "SYS_ADMIN".equals(operator.getRole()))) {
            return true;
        }
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, docId)
                .eq(DocumentMember::getUserId, operatorId)
                .eq(DocumentMember::getPermission, "ADMIN");
        return documentMemberMapper.selectCount(wrapper) > 0;
    }
}
