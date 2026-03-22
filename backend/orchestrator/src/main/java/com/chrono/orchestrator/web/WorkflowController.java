package com.chrono.orchestrator.web;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.orchestrator.domain.entity.WorkflowStatus;
import com.chrono.orchestrator.dto.StartWorkflowRequest;
import com.chrono.orchestrator.dto.WorkflowResponse;
import com.chrono.orchestrator.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController extends BaseController {

    private final WorkflowService workflowService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<WorkflowResponse> start(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody StartWorkflowRequest request) {
        return ApiResponse.success(workflowService.startWorkflow(request, tenantId, role));
    }

    @GetMapping("/{workflowId}")
    public ApiResponse<WorkflowResponse> get(
            @PathVariable String workflowId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(workflowService.getWorkflow(workflowId, tenantId, role));
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkflowResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResponse.success(workflowService.listWorkflows(tenantId, status, pageRequest, tenantId, role));
    }
}
