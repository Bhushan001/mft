package com.chrono.tenant.web;

import com.chrono.commons.constants.ApiConstants;
import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.tenant.domain.entity.TenantStatus;
import com.chrono.tenant.dto.CreateTenantRequest;
import com.chrono.tenant.dto.TenantResponse;
import com.chrono.tenant.dto.UpdateTenantRequest;
import com.chrono.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/tenants")
@RequiredArgsConstructor
public class TenantController extends BaseController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE) String role) {
        TenantResponse response = tenantService.createTenant(request, role);
        return created(response);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(
            @PathVariable String tenantId,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String callerTenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        TenantResponse response = tenantService.getTenantById(tenantId, callerTenantId, role);
        return ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TenantResponse>>> listTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status,
            @ModelAttribute PageRequest pageRequest,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE) String role) {
        PageResponse<TenantResponse> response = tenantService.listTenants(search, status, pageRequest, role);
        return page(response);
    }

    @PatchMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantRequest request,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String callerTenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        TenantResponse response = tenantService.updateTenant(tenantId, request, callerTenantId, role);
        return ok(response);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(
            @PathVariable String tenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE) String role) {
        tenantService.deleteTenant(tenantId, role);
        return noContent();
    }
}
