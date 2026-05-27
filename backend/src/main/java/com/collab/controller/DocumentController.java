package com.collab.controller;

import com.collab.common.Result;
import com.collab.dto.DocumentDTO;
import com.collab.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public Result<?> createDocument(@RequestBody DocumentDTO documentDTO) {
        return documentService.createDocument(documentDTO, getCurrentUserId());
    }

    @GetMapping("/{id}")
    public Result<?> getDocument(@PathVariable Long id) {
        return documentService.getDocument(id, getCurrentUserId());
    }

    @PutMapping("/{id}")
    public Result<?> updateDocument(@PathVariable Long id, @RequestBody DocumentDTO documentDTO) {
        return documentService.updateDocument(id, documentDTO, getCurrentUserId());
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteDocument(@PathVariable Long id) {
        return documentService.deleteDocument(id, getCurrentUserId());
    }

    @GetMapping("/list")
    public Result<?> listMyDocuments(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return documentService.listMyDocuments(getCurrentUserId(), page, size);
    }

    @PostMapping("/{id}/transfer")
    public Result<?> transferOwnership(@PathVariable Long id, @RequestBody java.util.Map<String, Long> body) {
        Long newOwnerId = body.get("newOwnerId");
        if (newOwnerId == null) {
            return Result.fail("请指定新的文档所有者");
        }
        return documentService.transferOwnership(id, newOwnerId, getCurrentUserId());
    }

    @GetMapping("/shared")
    public Result<?> listSharedDocuments(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return documentService.listSharedDocuments(getCurrentUserId(), page, size);
    }

    @GetMapping("/own")
    public Result<?> listOwnDocuments(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return documentService.listOwnDocuments(getCurrentUserId(), page, size);
    }

    @GetMapping("/stats")
    public Result<?> getDocumentStats() {
        return documentService.getDocumentStats(getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
