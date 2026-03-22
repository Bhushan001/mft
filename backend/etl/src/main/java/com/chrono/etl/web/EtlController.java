package com.chrono.etl.web;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.etl.domain.entity.EtlJobStatus;
import com.chrono.etl.dto.EtlJobResponse;
import com.chrono.etl.dto.SubmitEtlJobRequest;
import com.chrono.etl.service.EtlJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
public class EtlController extends BaseController {

    private final EtlJobService etlJobService;

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<EtlJobResponse> submit(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody SubmitEtlJobRequest request) {
        return ApiResponse.success(etlJobService.submitJob(request, tenantId, role));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<EtlJobResponse> getJob(
            @PathVariable String jobId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(etlJobService.getJob(jobId, tenantId, role));
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResponse<EtlJobResponse>> listJobs(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) EtlJobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResponse.success(etlJobService.listJobs(tenantId, status, pageRequest, tenantId, role));
    }
}
