package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.common.PageResult;
import com.collab.common.Result;
import com.collab.entity.Document;
import com.collab.entity.DocumentVersion;
import com.collab.entity.User;
import com.collab.mapper.DocumentMapper;
import com.collab.mapper.DocumentVersionMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.AuditLogService;
import com.collab.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VersionServiceImpl implements VersionService {

    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Result<?> createVersion(Long docId, Long userId, String changeSummary) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }

        int newVersionNumber = doc.getCurrentVersion() + 1;

        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(docId);
        version.setVersionNumber(newVersionNumber);
        version.setContent(doc.getContent());
        version.setOperatorId(userId);
        version.setChangeSummary(changeSummary);
        version.setDocSize(doc.getDocSize());
        documentVersionMapper.insert(version);

        doc.setCurrentVersion(newVersionNumber);
        documentMapper.updateById(doc);

        String username = getUsername(userId);
        auditLogService.log(userId, username, "CREATE", "DOCUMENT", docId,
                "保存版本 V" + newVersionNumber + ": " + doc.getTitle(), null);

        return Result.success(version);
    }

    @Override
    public Result<?> listVersions(Long docId, int page, int size) {
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, docId)
                .orderByDesc(DocumentVersion::getVersionNumber);
        Page<DocumentVersion> pageObj = new Page<>(page, size);
        Page<DocumentVersion> result = documentVersionMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    @Override
    public Result<?> getVersion(Long docId, Long versionId) {
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            return Result.fail("版本不存在");
        }
        if (!version.getDocumentId().equals(docId)) {
            return Result.fail("版本与文档不匹配");
        }
        return Result.success(version);
    }

    @Override
    public Result<?> compareVersions(Long docId, Long versionId1, Long versionId2) {
        DocumentVersion v1 = documentVersionMapper.selectById(versionId1);
        DocumentVersion v2 = documentVersionMapper.selectById(versionId2);
        if (v1 == null || v2 == null) {
            return Result.fail("版本不存在");
        }
        if (!v1.getDocumentId().equals(docId) || !v2.getDocumentId().equals(docId)) {
            return Result.fail("版本与文档不匹配");
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("version1", v1);
        comparison.put("version2", v2);
        return Result.success(comparison);
    }

    @Override
    @Transactional
    public Result<?> rollbackVersion(Long docId, Long versionId, Long userId) {
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            return Result.fail("版本不存在");
        }
        if (!version.getDocumentId().equals(docId)) {
            return Result.fail("版本与文档不匹配");
        }

        Document doc = documentMapper.selectById(docId);
        if (doc == null || doc.getStatus() == 0) {
            return Result.fail("文档不存在");
        }

        doc.setContent(version.getContent());
        doc.setDocSize(version.getDocSize());

        int newVersionNumber = doc.getCurrentVersion() + 1;
        doc.setCurrentVersion(newVersionNumber);
        documentMapper.updateById(doc);

        DocumentVersion rollbackVersion = new DocumentVersion();
        rollbackVersion.setDocumentId(docId);
        rollbackVersion.setVersionNumber(newVersionNumber);
        rollbackVersion.setContent(version.getContent());
        rollbackVersion.setOperatorId(userId);
        rollbackVersion.setChangeSummary("回滚至版本 " + version.getVersionNumber());
        rollbackVersion.setDocSize(version.getDocSize());
        documentVersionMapper.insert(rollbackVersion);

        String username = getUsername(userId);
        auditLogService.log(userId, username, "ROLLBACK", "DOCUMENT", docId,
                "回滚文档至版本 V" + version.getVersionNumber() + ": " + doc.getTitle(), null);

        return Result.success();
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }
}
