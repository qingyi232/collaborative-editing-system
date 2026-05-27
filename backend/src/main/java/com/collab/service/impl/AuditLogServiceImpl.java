package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.common.PageResult;
import com.collab.common.Result;
import com.collab.entity.AuditLog;
import com.collab.mapper.AuditLogMapper;
import com.collab.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void log(Long userId, String username, String action, String targetType, Long targetId, String detail, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ip);
        auditLogMapper.insert(log);
    }

    @Override
    public Result<?> listLogs(String action, String targetType, String username, String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(AuditLog::getTargetType, targetType);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(AuditLog::getUsername, username);
        }
        if (startDate != null && !startDate.isEmpty()) {
            try {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                wrapper.ge(AuditLog::getCreatedAt, start);
            } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
                wrapper.le(AuditLog::getCreatedAt, end);
            } catch (Exception ignored) {}
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> pageObj = new Page<>(page, size);
        Page<AuditLog> result = auditLogMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }
}
