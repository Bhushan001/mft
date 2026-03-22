package com.chrono.mapper.web;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.mapper.dto.CreateMappingRuleRequest;
import com.chrono.mapper.dto.MappingRuleResponse;
import com.chrono.mapper.dto.UpdateMappingRuleRequest;
import com.chrono.mapper.service.MappingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/mappings")
@RequiredArgsConstructor
public class MappingRuleController extends BaseController {

    private final MappingRuleService mappingRuleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MappingRuleResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateMappingRuleRequest request) {
        return ApiResponse.success(mappingRuleService.createMappingRule(request, tenantId));
    }

    /** Internal endpoint — used by Engine Service via Feign */
    @GetMapping("/active")
    public ApiResponse<MappingRuleResponse> getActive(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ApiResponse.success(mappingRuleService.getActiveMappingRule(tenantId));
    }

    @GetMapping("/{ruleId}")
    public ApiResponse<MappingRuleResponse> getById(
            @PathVariable String ruleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(mappingRuleService.getMappingRuleById(ruleId, tenantId, role));
    }

    @GetMapping
    public ApiResponse<PageResponse<MappingRuleResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResponse.success(mappingRuleService.listByTenant(tenantId, name, pageRequest, tenantId, role));
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<MappingRuleResponse> update(
            @PathVariable String ruleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody UpdateMappingRuleRequest request) {
        return ApiResponse.success(mappingRuleService.updateMappingRule(ruleId, request, tenantId, role));
    }

    @PostMapping("/{ruleId}/publish")
    public ApiResponse<MappingRuleResponse> publish(
            @PathVariable String ruleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(mappingRuleService.publishMappingRule(ruleId, tenantId, role));
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String ruleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Role") String role) {
        mappingRuleService.deleteMappingRule(ruleId, tenantId, role);
    }
}
