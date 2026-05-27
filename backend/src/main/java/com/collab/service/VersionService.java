package com.collab.service;

import com.collab.common.Result;

public interface VersionService {

    Result<?> createVersion(Long docId, Long userId, String changeSummary);

    Result<?> listVersions(Long docId, int page, int size);

    Result<?> getVersion(Long docId, Long versionId);

    Result<?> compareVersions(Long docId, Long versionId1, Long versionId2);

    Result<?> rollbackVersion(Long docId, Long versionId, Long userId);
}
