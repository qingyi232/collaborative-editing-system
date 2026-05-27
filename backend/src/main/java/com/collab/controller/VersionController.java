package com.collab.controller;

import com.collab.common.Result;
import com.collab.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc/{docId}/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @PostMapping
    public Result<?> createVersion(@PathVariable Long docId, @RequestBody(required = false) java.util.Map<String, String> body) {
        String changeSummary = body != null ? body.getOrDefault("changeSummary", "手动保存") : "手动保存";
        return versionService.createVersion(docId, getCurrentUserId(), changeSummary);
    }

    @GetMapping("/list")
    public Result<?> listVersions(@PathVariable Long docId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return versionService.listVersions(docId, page, size);
    }

    @GetMapping("/{versionId}")
    public Result<?> getVersion(@PathVariable Long docId, @PathVariable Long versionId) {
        return versionService.getVersion(docId, versionId);
    }

    @GetMapping("/compare")
    public Result<?> compareVersions(@PathVariable Long docId, @RequestParam Long v1, @RequestParam Long v2) {
        return versionService.compareVersions(docId, v1, v2);
    }

    @PostMapping("/{versionId}/rollback")
    public Result<?> rollbackVersion(@PathVariable Long docId, @PathVariable Long versionId) {
        return versionService.rollbackVersion(docId, versionId, getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
