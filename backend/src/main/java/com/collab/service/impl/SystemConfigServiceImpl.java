package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.Result;
import com.collab.dto.SystemConfigDTO;
import com.collab.entity.SystemConfig;
import com.collab.entity.User;
import com.collab.mapper.SystemConfigMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.AuditLogService;
import com.collab.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    @Override
    public Result<?> getAllConfigs() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        return Result.success(configs);
    }

    @Override
    public Result<?> updateConfig(SystemConfigDTO dto, Long userId) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, dto.getConfigKey());
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config == null) {
            return Result.fail("配置项不存在");
        }
        String oldValue = config.getConfigValue();
        config.setConfigValue(dto.getConfigValue());
        config.setUpdatedBy(userId);
        systemConfigMapper.updateById(config);

        User user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : null;
        auditLogService.log(userId, username, "UPDATE_CONFIG", "SYSTEM", config.getId(),
                "修改系统配置: " + dto.getConfigKey() + " [" + oldValue + " → " + dto.getConfigValue() + "]", null);

        return Result.success();
    }

    @Override
    public String getConfigValue(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }
}
