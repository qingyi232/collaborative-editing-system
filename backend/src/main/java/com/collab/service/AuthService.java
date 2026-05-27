package com.collab.service;

import com.collab.common.Result;
import com.collab.dto.LoginDTO;
import com.collab.dto.RegisterDTO;

public interface AuthService {

    Result<?> login(LoginDTO dto, String ip);

    Result<?> register(RegisterDTO dto, String ip);

    Result<?> getUserInfo(Long userId);

    Result<?> updateProfile(Long userId, java.util.Map<String, String> profileData);

    Result<?> changePassword(Long userId, String oldPassword, String newPassword);

    Result<?> searchUsers(String keyword, Long currentUserId);
}
