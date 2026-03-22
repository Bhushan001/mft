package com.chrono.engine.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.engine.client.dto.MappingRuleDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MapperServiceClientFallback implements MapperServiceClient {

    @Override
    public ApiResponse<MappingRuleDto> getActiveMappingRule(String tenantId) {
        log.warn("Mapper service unavailable — fallback triggered for tenantId={}", tenantId);
        return ApiResponse.error("Mapper service unavailable", 503);
    }
}
