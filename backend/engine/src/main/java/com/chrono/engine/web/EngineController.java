package com.chrono.engine.web;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.engine.dto.ProcessRequest;
import com.chrono.engine.dto.ProcessResponse;
import com.chrono.engine.service.EngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class EngineController extends BaseController {

    private final EngineService engineService;

    @PostMapping("/process")
    public ApiResponse<ProcessResponse> process(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ProcessRequest request) {
        return ApiResponse.success(engineService.process(request, tenantId, role));
    }

    @GetMapping("/requests/{requestId}")
    public ApiResponse<ProcessResponse> getById(
            @PathVariable String requestId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(engineService.getRequestById(requestId, tenantId, role));
    }

    @GetMapping("/requests")
    public ApiResponse<PageResponse<ProcessResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResponse.success(engineService.listRequests(tenantId, pageRequest, tenantId, role));
    }
}
