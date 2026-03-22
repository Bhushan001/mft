package com.chrono.etl.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.etl.client.dto.ProcessRequestDto;
import com.chrono.etl.client.dto.ProcessResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "engine-service", fallback = EngineServiceClientFallback.class)
public interface EngineServiceClient {

    @PostMapping("/api/v1/engine/process")
    ApiResponse<ProcessResponseDto> process(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody ProcessRequestDto request);
}
