package com.chrono.tenant.service;

import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.tenant.domain.entity.Tenant;
import com.chrono.tenant.domain.entity.TenantStatus;
import com.chrono.tenant.domain.repository.TenantRepository;
import com.chrono.tenant.dto.CreateTenantRequest;
import com.chrono.tenant.dto.TenantResponse;
import com.chrono.tenant.dto.UpdateTenantRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService extends BaseService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request, String callerRole) {
        if (!UserRole.PLATFORM_ADMIN.name().equals(callerRole)) {
            throw new BusinessException("Only PLATFORM_ADMIN can create tenants");
        }

        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Tenant", "slug", request.getSlug());
        }

        Tenant tenant = new Tenant();
        tenant.setTenantId(UUID.randomUUID().toString());
        tenant.setName(request.getName());
        tenant.setSlug(request.getSlug());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setPlan(request.getPlan() != null ? request.getPlan() : "FREE");
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");

        tenantRepository.save(tenant);
        log.info("Tenant created: tenantId={}, slug={}", tenant.getTenantId(), tenant.getSlug());
        return TenantResponse.from(tenant);
    }

    @Cacheable(value = "tenants", key = "#tenantId")
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(String tenantId, String callerTenantId, String callerRole) {
        Tenant tenant = findActiveTenant(tenantId);
        assertTenantAccess(tenant.getTenantId(), callerTenantId, callerRole);
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> listTenants(
            String search, TenantStatus status, PageRequest pageRequest, String callerRole) {

        // Only PLATFORM_ADMIN can list all tenants
        if (!UserRole.PLATFORM_ADMIN.name().equals(callerRole)) {
            throw new BusinessException("Only PLATFORM_ADMIN can list all tenants");
        }

        org.springframework.data.domain.Page<Tenant> page =
                tenantRepository.searchTenants(search, status, pageRequest.toSpringPageRequest());
        return PageResponse.from(page.map(TenantResponse::from));
    }

    @CacheEvict(value = "tenants", key = "#tenantId")
    @Transactional
    public TenantResponse updateTenant(
            String tenantId, UpdateTenantRequest request, String callerTenantId, String callerRole) {

        Tenant tenant = findActiveTenant(tenantId);
        assertTenantAccess(tenant.getTenantId(), callerTenantId, callerRole);

        if (request.getName() != null)         tenant.setName(request.getName());
        if (request.getContactEmail() != null) tenant.setContactEmail(request.getContactEmail());
        if (request.getPlan() != null)         tenant.setPlan(request.getPlan());
        if (request.getMaxUsers() != null)     tenant.setMaxUsers(request.getMaxUsers());
        if (request.getTimezone() != null)     tenant.setTimezone(request.getTimezone());

        // Status changes restricted to PLATFORM_ADMIN
        if (request.getStatus() != null) {
            if (!UserRole.PLATFORM_ADMIN.name().equals(callerRole)) {
                throw new BusinessException("Only PLATFORM_ADMIN can change tenant status");
            }
            tenant.setStatus(request.getStatus());
        }

        log.info("Tenant updated: tenantId={}", tenantId);
        return TenantResponse.from(tenant);
    }

    @CacheEvict(value = "tenants", key = "#tenantId")
    @Transactional
    public void deleteTenant(String tenantId, String callerRole) {
        if (!UserRole.PLATFORM_ADMIN.name().equals(callerRole)) {
            throw new BusinessException("Only PLATFORM_ADMIN can delete tenants");
        }

        Tenant tenant = findActiveTenant(tenantId);
        tenant.setDeletedAt(LocalDateTime.now());
        log.info("Tenant soft-deleted: tenantId={}", tenantId);
    }

    private Tenant findActiveTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }
}
