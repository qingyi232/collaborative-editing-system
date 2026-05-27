package com.collab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.Result;
import com.collab.dto.SystemConfigDTO;
import com.collab.entity.AuditLog;
import com.collab.mapper.AuditLogMapper;
import com.collab.service.AuditLogService;
import com.collab.service.SystemConfigService;
import com.collab.websocket.CollabWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping("/logs")
    public Result<?> listLogs(@RequestParam(required = false) String action,
                               @RequestParam(required = false) String targetType,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return auditLogService.listLogs(action, targetType, username, startDate, endDate, page, size);
    }

    @GetMapping("/configs")
    public Result<?> getAllConfigs() {
        return systemConfigService.getAllConfigs();
    }

    @PutMapping("/config")
    public Result<?> updateConfig(@RequestBody SystemConfigDTO configDTO) {
        return systemConfigService.updateConfig(configDTO, getCurrentUserId());
    }

    @GetMapping("/monitor")
    public Result<?> getMonitorData() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jvmMemoryUsed", heapUsed);
        data.put("jvmMemoryMax", heapMax);
        data.put("jvmMemoryUsage", heapMax > 0 ? Math.round((double) heapUsed / heapMax * 10000) / 100.0 : 0);
        data.put("cpuCores", Runtime.getRuntime().availableProcessors());
        data.put("activeThreads", Thread.activeCount());
        data.put("onlineConnections", CollabWebSocketHandler.getOnlineCount());

        return Result.success(data);
    }

    @GetMapping("/logs/export")
    public void exportLogs(@RequestParam(required = false) String action,
                           @RequestParam(required = false) String targetType,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) wrapper.eq(AuditLog::getAction, action);
        if (targetType != null && !targetType.isEmpty()) wrapper.eq(AuditLog::getTargetType, targetType);
        if (username != null && !username.isEmpty()) wrapper.like(AuditLog::getUsername, username);
        if (startDate != null && !startDate.isEmpty()) {
            try { wrapper.ge(AuditLog::getCreatedAt, LocalDate.parse(startDate).atStartOfDay()); } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isEmpty()) {
            try { wrapper.le(AuditLog::getCreatedAt, LocalDate.parse(endDate).atTime(LocalTime.MAX)); } catch (Exception ignored) {}
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        List<AuditLog> logs = auditLogMapper.selectList(wrapper);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=audit_logs.csv");
        response.setCharacterEncoding("UTF-8");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");
        writer.println("时间,用户,操作类型,目标类型,详情,IP地址");
        for (AuditLog log : logs) {
            String time = log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "";
            String u = log.getUsername() != null ? log.getUsername() : "";
            String act = log.getAction() != null ? log.getAction() : "";
            String tt = log.getTargetType() != null ? log.getTargetType() : "";
            String detail = log.getDetail() != null ? log.getDetail().replace("\"", "\"\"") : "";
            String ip = log.getIpAddress() != null ? log.getIpAddress() : "";
            writer.println(time + "," + u + "," + act + "," + tt + ",\"" + detail + "\"," + ip);
        }
        writer.flush();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
