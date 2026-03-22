package com.chrono.audit.service;

import com.chrono.audit.domain.entity.AuditEvent;
import com.chrono.audit.domain.repository.AuditEventRepository;
import com.chrono.audit.dto.AuditEventResponse;
import com.chrono.audit.dto.RecordAuditRequest;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    /**
     * Records a new audit event. Called internally by other services.
     * This endpoint must only be accessible from within the cluster (no public gateway route).
     */
    @Transactional
    public AuditEventResponse record(RecordAuditRequest request) {
        AuditEvent event = new AuditEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setAction(request.getAction().toUpperCase());
        event.setResourceType(request.getResourceType());
        event.setResourceId(request.getResourceId());
        event.setPerformedBy(request.getPerformedBy());
        event.setTenantId(request.getTenantId());
        event.setDetails(request.getDetails());
        event.setIpAddress(request.getIpAddress());

        auditEventRepository.save(event);
        log.info("Audit event recorded: action={}, resourceType={}, performedBy={}, tenantId={}",
                event.getAction(), event.getResourceType(), event.getPerformedBy(), event.getTenantId());

        return AuditEventResponse.from(event);
    }

    /**
     * Queries audit events with optional filters.
     * PLATFORM_ADMIN sees all tenants; TENANT_ADMIN is restricted to their own tenantId.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(
            String tenantId,
            String action,
            String resourceType,
            String performedBy,
            PageRequest pageRequest,
            String callerTenantId,
            String callerRole) {

        String resolvedTenantId = resolvedTenantFilter(tenantId, callerTenantId, callerRole);

        Page<AuditEvent> page = auditEventRepository.search(
                resolvedTenantId,
                action,
                resourceType,
                performedBy,
                pageRequest.toSpringPageRequest());

        return PageResponse.from(page.map(AuditEventResponse::from));
    }

    /**
     * PLATFORM_ADMIN may pass an explicit tenantId filter (or null for all tenants).
     * All other roles are restricted to their own tenant.
     */
    private String resolvedTenantFilter(String requestedTenantId,
                                        String callerTenantId,
                                        String callerRole) {
        if (UserRole.PLATFORM_ADMIN.name().equals(callerRole)) {
            return requestedTenantId; // null = all tenants; specific = filtered
        }
        // Non-admin: silently enforce own tenant regardless of what was requested
        if (requestedTenantId != null && !requestedTenantId.equals(callerTenantId)) {
            throw new BusinessException("You can only query audit events for your own tenant");
        }
        return callerTenantId;
    }
}
