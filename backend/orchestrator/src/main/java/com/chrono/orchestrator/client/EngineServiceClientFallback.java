package com.chrono.orchestrator.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.orchestrator.client.dto.ProcessRequestDto;
import com.chrono.orchestrator.client.dto.ProcessResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EngineServiceClientFallback implements EngineServiceClient {

    @Override
    public ApiResponse<ProcessResponseDto> process(String tenantId, String role, ProcessRequestDto request) {
        log.warn("Engine service unavailable — fallback triggered for tenantId={}", tenantId);
        return ApiResponse.error("Engine service unavailable", 503);
    }
}
