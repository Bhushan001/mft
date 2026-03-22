package com.chrono.audit.controller;

import com.chrono.audit.dto.AuditEventResponse;
import com.chrono.audit.dto.RecordAuditRequest;
import com.chrono.audit.service.AuditService;
import com.chrono.commons.constants.ApiConstants;
import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID   = "X-User-Id";
    private static final String HEADER_ROLE      = "X-User-Role";

    private final AuditService auditService;

    /**
     * Internal endpoint — records an audit event.
     * Not exposed via API Gateway; only called service-to-service.
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuditEventResponse> record(
            @Valid @RequestBody RecordAuditRequest request) {
        return ApiResponse.success(auditService.record(request));
    }

    /**
     * GET /api/v1/audit/events
     * Paginated, filterable audit log. Exposed via API Gateway (PLATFORM_ADMIN + TENANT_ADMIN).
     */
    @GetMapping("/events")
    public ApiResponse<PageResponse<AuditEventResponse>> search(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String performedBy,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(HEADER_TENANT_ID) String callerTenantId,
            @RequestHeader(HEADER_ROLE)      String callerRole) {

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);

        return ApiResponse.success(
                auditService.search(tenantId, action, resourceType, performedBy,
                        pageRequest, callerTenantId, callerRole));
    }
}
