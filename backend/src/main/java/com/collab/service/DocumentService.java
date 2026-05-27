package com.collab.service;

import com.collab.common.Result;
import com.collab.dto.DocumentDTO;

public interface DocumentService {

    Result<?> createDocument(DocumentDTO dto, Long userId);

    Result<?> getDocument(Long docId, Long userId);

    Result<?> updateDocument(Long docId, DocumentDTO dto, Long userId);

    Result<?> deleteDocument(Long docId, Long userId);

    Result<?> listMyDocuments(Long userId, int page, int size);

    Result<?> transferOwnership(Long docId, Long newOwnerId, Long currentUserId);

    Result<?> listSharedDocuments(Long userId, int page, int size);

    Result<?> listOwnDocuments(Long userId, int page, int size);

    Result<?> getDocumentStats(Long userId);

    Result<?> listAllDocuments(int page, int size, String keyword);

    Result<?> updateDocumentStatus(Long docId, Integer status, Long operatorId);

    Result<?> getDocAdminStats();
}
