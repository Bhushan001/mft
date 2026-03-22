package com.chrono.orchestrator.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.orchestrator.client.dto.EtlJobDto;
import com.chrono.orchestrator.client.dto.SubmitEtlJobDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EtlServiceClientFallback implements EtlServiceClient {

    @Override
    public ApiResponse<EtlJobDto> submitJob(String tenantId, String role, SubmitEtlJobDto request) {
        log.warn("ETL service unavailable — fallback triggered for tenantId={}", tenantId);
        return ApiResponse.error("ETL service unavailable", 503);
    }

    @Override
    public ApiResponse<EtlJobDto> getJob(String tenantId, String jobId) {
        log.warn("ETL service unavailable — fallback triggered for jobId={}", jobId);
        return ApiResponse.error("ETL service unavailable", 503);
    }
}
