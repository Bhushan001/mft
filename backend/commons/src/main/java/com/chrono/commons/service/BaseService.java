package com.chrono.commons.service;

import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.TenantViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseService {

    /**
     * Enforces tenant isolation on every data access.
     * PLATFORM_ADMIN bypasses the tenant check.
     * Throws TenantViolationException (HTTP 403) on mismatch.
     */
    protected void assertTenantAccess(String resourceTenantId,
                                      String requestingTenantId,
                                      String requestingRole) {
        if (UserRole.PLATFORM_ADMIN.name().equals(requestingRole)) {
            return;
        }
        if (!resourceTenantId.equals(requestingTenantId)) {
            log.warn("Tenant violation: requestingTenantId={} attempted access to resource owned by tenantId={}",
                    requestingTenantId, resourceTenantId);
            throw new TenantViolationException(
                    "Access denied: you do not have permission to access this resource");
        }
    }

    /**
     * Validates that the tenant context header is present.
     * Prevents accidental bypass when gateway header is missing.
     */
    protected void requireTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new TenantViolationException(
                    "Tenant context is missing. Ensure request passes through the API Gateway.");
        }
    }
}
