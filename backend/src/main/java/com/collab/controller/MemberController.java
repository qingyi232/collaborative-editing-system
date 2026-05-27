package com.collab.controller;

import com.collab.common.Result;
import com.collab.dto.MemberDTO;
import com.collab.service.DocumentMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc/{docId}/member")
@RequiredArgsConstructor
public class MemberController {

    private final DocumentMemberService documentMemberService;

    @PostMapping
    public Result<?> addMember(@PathVariable Long docId, @RequestBody MemberDTO memberDTO) {
        return documentMemberService.addMember(docId, memberDTO, getCurrentUserId());
    }

    @DeleteMapping("/{userId}")
    public Result<?> removeMember(@PathVariable Long docId, @PathVariable Long userId) {
        return documentMemberService.removeMember(docId, userId, getCurrentUserId());
    }

    @PutMapping
    public Result<?> updatePermission(@PathVariable Long docId, @RequestBody MemberDTO memberDTO) {
        return documentMemberService.updatePermission(docId, memberDTO, getCurrentUserId());
    }

    @GetMapping("/list")
    public Result<?> listMembers(@PathVariable Long docId) {
        return documentMemberService.listMembers(docId);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
