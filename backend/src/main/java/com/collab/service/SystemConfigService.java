package com.collab.service;

import com.collab.common.Result;
import com.collab.dto.SystemConfigDTO;

public interface SystemConfigService {

    Result<?> getAllConfigs();

    Result<?> updateConfig(SystemConfigDTO dto, Long userId);

    String getConfigValue(String key);
}
