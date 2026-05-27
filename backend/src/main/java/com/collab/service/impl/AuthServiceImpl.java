package com.collab.service.impl;

import com.collab.common.Result;
import com.collab.dto.LoginDTO;
import com.collab.dto.RegisterDTO;
import com.collab.entity.User;
import com.collab.mapper.UserMapper;
import com.collab.service.AuditLogService;
import com.collab.service.AuthService;
import com.collab.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    public Result<?> login(LoginDTO dto, String ip) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail("账号已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "USER", user.getId(),
                "用户登录: " + user.getUsername(), ip);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());
        return Result.success(data);
    }

    @Override
    public Result<?> register(RegisterDTO dto, String ip) {
        User existing = userMapper.selectByUsername(dto.getUsername());
        if (existing != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        auditLogService.log(user.getId(), user.getUsername(), "REGISTER", "USER", user.getId(),
                "新用户注册: " + user.getUsername(), ip);

        return Result.success();
    }

    @Override
    public Result<?> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<?> updateProfile(Long userId, Map<String, String> profileData) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (profileData.containsKey("nickname")) {
            user.setNickname(profileData.get("nickname"));
        }
        if (profileData.containsKey("email")) {
            user.setEmail(profileData.get("email"));
        }
        userMapper.updateById(user);

        auditLogService.log(userId, user.getUsername(), "UPDATE_PROFILE", "USER", userId,
                "更新个人资料", null);

        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null) {
            return Result.fail("请填写完整的密码信息");
        }
        if (newPassword.length() < 6) {
            return Result.fail("新密码至少6个字符");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.fail("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        auditLogService.log(userId, user.getUsername(), "CHANGE_PASSWORD", "USER", userId,
                "修改密码", null);

        return Result.success();
    }

    @Override
    public Result<?> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(List.of());
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword))
                .ne(User::getId, currentUserId)
                .eq(User::getStatus, 1)
                .last("LIMIT 10");
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
