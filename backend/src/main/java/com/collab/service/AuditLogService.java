package com.collab.service;

import com.collab.common.Result;

public interface AuditLogService {

    void log(Long userId, String username, String action, String targetType, Long targetId, String detail, String ip);

    Result<?> listLogs(String action, String targetType, String username, String startDate, String endDate, int page, int size);
}
