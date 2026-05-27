package com.collab.controller;

import com.collab.common.Result;
import com.collab.dto.LoginDTO;
import com.collab.dto.RegisterDTO;
import com.collab.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        return authService.login(loginDTO, getClientIp(request));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO registerDTO, HttpServletRequest request) {
        return authService.register(registerDTO, getClientIp(request));
    }

    @GetMapping("/info")
    public Result<?> getUserInfo() {
        return authService.getUserInfo(getCurrentUserId());
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody java.util.Map<String, String> body) {
        return authService.updateProfile(getCurrentUserId(), body);
    }

    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody java.util.Map<String, String> body) {
        return authService.changePassword(getCurrentUserId(), body.get("oldPassword"), body.get("newPassword"));
    }

    @GetMapping("/search")
    public Result<?> searchUsers(@RequestParam String keyword) {
        return authService.searchUsers(keyword, getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
