package com.collab.controller;

import com.collab.common.Result;
import com.collab.dto.MemberDTO;
import com.collab.service.DocumentMemberService;
import com.collab.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docadmin")
@RequiredArgsConstructor
public class DocAdminController {

    private final DocumentService documentService;
    private final DocumentMemberService documentMemberService;

    @GetMapping("/documents")
    public Result<?> listAllDocuments(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword) {
        return documentService.listAllDocuments(page, size, keyword);
    }

    @GetMapping("/documents/{docId}/members")
    public Result<?> listMembers(@PathVariable Long docId) {
        return documentMemberService.listMembers(docId);
    }

    @PostMapping("/documents/{docId}/members")
    public Result<?> addMember(@PathVariable Long docId, @RequestBody MemberDTO dto) {
        return documentMemberService.addMember(docId, dto, getCurrentUserId());
    }

    @DeleteMapping("/documents/{docId}/members/{userId}")
    public Result<?> removeMember(@PathVariable Long docId, @PathVariable Long userId) {
        return documentMemberService.removeMember(docId, userId, getCurrentUserId());
    }

    @PutMapping("/documents/{docId}/members")
    public Result<?> updateMemberPermission(@PathVariable Long docId, @RequestBody MemberDTO dto) {
        return documentMemberService.updatePermission(docId, dto, getCurrentUserId());
    }

    @PostMapping("/documents/{docId}/transfer")
    public Result<?> transferOwnership(@PathVariable Long docId,
                                        @RequestBody java.util.Map<String, Long> body) {
        Long newOwnerId = body.get("newOwnerId");
        if (newOwnerId == null) {
            return Result.fail("请指定新的文档所有者");
        }
        return documentService.transferOwnership(docId, newOwnerId, getCurrentUserId());
    }

    @PutMapping("/documents/{docId}/status")
    public Result<?> updateDocumentStatus(@PathVariable Long docId,
                                           @RequestBody java.util.Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail("请指定文档状态");
        }
        return documentService.updateDocumentStatus(docId, status, getCurrentUserId());
    }

    @GetMapping("/stats")
    public Result<?> getDocAdminStats() {
        return documentService.getDocAdminStats();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
