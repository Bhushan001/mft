package com.chrono.orchestrator.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.orchestrator.client.dto.EtlJobDto;
import com.chrono.orchestrator.client.dto.SubmitEtlJobDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "etl-service", fallback = EtlServiceClientFallback.class)
public interface EtlServiceClient {

    @PostMapping("/api/v1/etl/jobs")
    ApiResponse<EtlJobDto> submitJob(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody SubmitEtlJobDto request);

    @GetMapping("/api/v1/etl/jobs/{jobId}")
    ApiResponse<EtlJobDto> getJob(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable("jobId") String jobId);
}
