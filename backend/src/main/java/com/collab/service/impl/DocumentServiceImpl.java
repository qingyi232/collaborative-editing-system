package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.PageResult;
import com.collab.common.Result;
import com.collab.dto.DocumentDTO;
import com.collab.entity.Document;
import com.collab.entity.DocumentVersion;
import com.collab.entity.DocumentMember;
import com.collab.entity.User;
import com.collab.mapper.DocumentMapper;
import com.collab.mapper.DocumentMemberMapper;
import com.collab.mapper.DocumentVersionMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.AuditLogService;
import com.collab.service.DocumentService;
import com.collab.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentMemberMapper documentMemberMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;

    @Override
    @Transactional
    public Result<?> createDocument(DocumentDTO dto, Long userId) {
        String maxStorageStr = systemConfigService.getConfigValue("max_storage_per_user");
        if (maxStorageStr != null) {
            try {
                long maxStorage = Long.parseLong(maxStorageStr);
                long currentUsage = calculateUserStorage(userId);
                if (currentUsage >= maxStorage) {
                    return Result.fail("存储空间已满（最大 " + (maxStorage / 1024 / 1024) + "MB），请删除部分文档后重试");
                }
            } catch (NumberFormatException ignored) {}
        }

        Document doc = new Document();
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent() != null ? dto.getContent() : "");
        doc.setOwnerId(userId);
        doc.setStatus(1);
        doc.setIsPublic(dto.getIsPublic() != null && dto.getIsPublic() ? 1 : 0);
        doc.setCurrentVersion(1);
        doc.setDocSize(doc.getContent() != null ? (long) doc.getContent().getBytes().length : 0L);
        documentMapper.insert(doc);

        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(doc.getId());
        version.setVersionNumber(1);
        version.setContent(doc.getContent());
        version.setOperatorId(userId);
        version.setChangeSummary("初始版本");
        version.setDocSize(doc.getDocSize());
        documentVersionMapper.insert(version);

        String username = getUsername(userId);
        auditLogService.log(userId, username, "CREATE", "DOCUMENT", doc.getId(),
                "创建文档: " + doc.getTitle(), null);

        return Result.success(doc);
    }

    @Override
    public Result<?> getDocument(Long docId, Long userId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }
        if (!hasAccess(doc, userId)) {
            return Result.fail("无权访问该文档");
        }
        return Result.success(doc);
    }

    @Override
    @Transactional
    public Result<?> updateDocument(Long docId, DocumentDTO dto, Long userId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }
        if (!hasEditAccess(doc, userId)) {
            return Result.fail(403, "无权编辑该文档");
        }

        if (dto.getTitle() != null) {
            doc.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            long newSize = (long) dto.getContent().getBytes().length;

            String maxDocSizeStr = systemConfigService.getConfigValue("max_doc_size");
            if (maxDocSizeStr != null) {
                try {
                    long maxDocSize = Long.parseLong(maxDocSizeStr);
                    if (newSize > maxDocSize) {
                        return Result.fail("文档大小超过限制（最大 " + (maxDocSize / 1024 / 1024) + "MB）");
                    }
                } catch (NumberFormatException ignored) {}
            }

            String maxStorageStr = systemConfigService.getConfigValue("max_storage_per_user");
            if (maxStorageStr != null) {
                try {
                    long maxStorage = Long.parseLong(maxStorageStr);
                    long currentUsage = calculateUserStorage(doc.getOwnerId());
                    long oldDocSize = doc.getDocSize() != null ? doc.getDocSize() : 0L;
                    long projectedUsage = currentUsage - oldDocSize + newSize;
                    if (projectedUsage > maxStorage) {
                        return Result.fail("存储空间不足（最大 " + (maxStorage / 1024 / 1024) + "MB）");
                    }
                } catch (NumberFormatException ignored) {}
            }

            doc.setContent(dto.getContent());
            doc.setDocSize(newSize);
        }
        if (dto.getIsPublic() != null) {
            doc.setIsPublic(dto.getIsPublic() ? 1 : 0);
        }
        documentMapper.updateById(doc);

        String username = getUsername(userId);
        auditLogService.log(userId, username, "UPDATE", "DOCUMENT", doc.getId(),
                "更新文档: " + doc.getTitle(), null);

        return Result.success(doc);
    }

    @Override
    @Transactional
    public Result<?> deleteDocument(Long docId, Long userId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }
        if (!doc.getOwnerId().equals(userId) && !isPrivilegedUser(userId)) {
            return Result.fail("只有文档所有者可以删除文档");
        }
        doc.setStatus(0);
        documentMapper.updateById(doc);

        String username = getUsername(userId);
        auditLogService.log(userId, username, "DELETE", "DOCUMENT", doc.getId(),
                "删除文档: " + doc.getTitle(), null);

        return Result.success();
    }

    @Override
    public Result<?> listMyDocuments(Long userId, int page, int size) {
        List<Document> allDocs = documentMapper.selectUserDocuments(userId);
        int total = allDocs.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Document> pageList = allDocs.subList(from, to);
        return Result.success(PageResult.of(pageList, total, size, page));
    }

    @Override
    @Transactional
    public Result<?> transferOwnership(Long docId, Long newOwnerId, Long currentUserId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }
        boolean isOwner = doc.getOwnerId().equals(currentUserId);
        boolean isDocAdmin = false;
        User operator = userMapper.selectById(currentUserId);
        if (operator != null && ("DOC_ADMIN".equals(operator.getRole()) || "SYS_ADMIN".equals(operator.getRole()))) {
            isDocAdmin = true;
        }
        if (!isOwner && !isDocAdmin) {
            return Result.fail("只有文档所有者或文档管理员可以转让所有权");
        }
        doc.setOwnerId(newOwnerId);
        documentMapper.updateById(doc);

        String username = getUsername(currentUserId);
        String newOwnerName = getUsername(newOwnerId);
        auditLogService.log(currentUserId, username, "TRANSFER", "DOCUMENT", doc.getId(),
                "转移文档「" + doc.getTitle() + "」所有权给用户: " + (newOwnerName != null ? newOwnerName : newOwnerId), null);

        return Result.success();
    }

    @Override
    public Result<?> listSharedDocuments(Long userId, int page, int size) {
        List<Document> allDocs = documentMapper.selectSharedDocuments(userId);
        int total = allDocs.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Document> pageList = allDocs.subList(from, to);
        return Result.success(PageResult.of(pageList, total, size, page));
    }

    @Override
    public Result<?> listOwnDocuments(Long userId, int page, int size) {
        List<Document> allDocs = documentMapper.selectOwnDocuments(userId);
        int total = allDocs.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Document> pageList = allDocs.subList(from, to);
        return Result.success(PageResult.of(pageList, total, size, page));
    }

    @Override
    public Result<?> getDocumentStats(Long userId) {
        int ownCount = documentMapper.selectOwnDocuments(userId).size();
        int sharedCount = documentMapper.selectSharedDocuments(userId).size();
        int totalCount = documentMapper.selectUserDocuments(userId).size();
        Map<String, Object> stats = new HashMap<>();
        stats.put("ownCount", ownCount);
        stats.put("sharedCount", sharedCount);
        stats.put("totalCount", totalCount);
        return Result.success(stats);
    }

    private boolean hasAccess(Document doc, Long userId) {
        if (doc.getOwnerId().equals(userId)) {
            return true;
        }
        if (isPrivilegedUser(userId)) {
            return true;
        }
        if (doc.getIsPublic() != null && doc.getIsPublic() == 1) {
            return true;
        }
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, doc.getId())
                .eq(DocumentMember::getUserId, userId);
        return documentMemberMapper.selectCount(wrapper) > 0;
    }

    private boolean hasEditAccess(Document doc, Long userId) {
        if (doc.getOwnerId().equals(userId)) {
            return true;
        }
        if (isPrivilegedUser(userId)) {
            return true;
        }
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, doc.getId())
                .eq(DocumentMember::getUserId, userId)
                .in(DocumentMember::getPermission, "EDIT", "ADMIN");
        return documentMemberMapper.selectCount(wrapper) > 0;
    }

    private boolean isPrivilegedUser(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && ("DOC_ADMIN".equals(user.getRole()) || "SYS_ADMIN".equals(user.getRole()));
    }

    private long calculateUserStorage(Long userId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getOwnerId, userId).ne(Document::getStatus, 0);
        List<Document> docs = documentMapper.selectList(wrapper);
        return docs.stream().mapToLong(d -> d.getDocSize() != null ? d.getDocSize() : 0L).sum();
    }

    @Override
    public Result<?> listAllDocuments(int page, int size, String keyword) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Document::getStatus, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Document::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Document::getUpdatedAt);

        List<Document> allDocs = documentMapper.selectList(wrapper);
        int total = allDocs.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Document> pageList = allDocs.subList(from, to);

        List<Map<String, Object>> enrichedList = new java.util.ArrayList<>();
        for (Document doc : pageList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("title", doc.getTitle());
            map.put("ownerId", doc.getOwnerId());
            map.put("status", doc.getStatus());
            map.put("isPublic", doc.getIsPublic());
            map.put("currentVersion", doc.getCurrentVersion());
            map.put("docSize", doc.getDocSize());
            map.put("createdAt", doc.getCreatedAt());
            map.put("updatedAt", doc.getUpdatedAt());

            User owner = userMapper.selectById(doc.getOwnerId());
            map.put("ownerName", owner != null ? (owner.getNickname() != null ? owner.getNickname() : owner.getUsername()) : "未知");

            LambdaQueryWrapper<DocumentMember> mw = new LambdaQueryWrapper<>();
            mw.eq(DocumentMember::getDocumentId, doc.getId());
            map.put("memberCount", documentMemberMapper.selectCount(mw));

            enrichedList.add(map);
        }

        return Result.success(PageResult.of(enrichedList, total, size, page));
    }

    @Override
    @Transactional
    public Result<?> updateDocumentStatus(Long docId, Integer status, Long operatorId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            return Result.fail("文档不存在");
        }
        doc.setStatus(status);
        documentMapper.updateById(doc);

        String statusName = status == 0 ? "删除" : status == 1 ? "正常" : "归档";
        String username = getUsername(operatorId);
        auditLogService.log(operatorId, username, "UPDATE_STATUS", "DOCUMENT", docId,
                "修改文档「" + doc.getTitle() + "」状态为: " + statusName, null);

        return Result.success();
    }

    @Override
    public Result<?> getDocAdminStats() {
        LambdaQueryWrapper<Document> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.ne(Document::getStatus, 0);
        long totalDocs = documentMapper.selectCount(totalWrapper);

        LambdaQueryWrapper<Document> publicWrapper = new LambdaQueryWrapper<>();
        publicWrapper.ne(Document::getStatus, 0).eq(Document::getIsPublic, 1);
        long publicDocs = documentMapper.selectCount(publicWrapper);

        LambdaQueryWrapper<Document> archivedWrapper = new LambdaQueryWrapper<>();
        archivedWrapper.eq(Document::getStatus, 2);
        long archivedDocs = documentMapper.selectCount(archivedWrapper);

        LambdaQueryWrapper<DocumentMember> memberWrapper = new LambdaQueryWrapper<>();
        long totalMembers = documentMemberMapper.selectCount(memberWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocs", totalDocs);
        stats.put("publicDocs", publicDocs);
        stats.put("archivedDocs", archivedDocs);
        stats.put("totalMembers", totalMembers);
        return Result.success(stats);
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }
}
