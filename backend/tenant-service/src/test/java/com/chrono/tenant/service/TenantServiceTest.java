package com.chrono.tenant.service;

import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.exception.TenantViolationException;
import com.chrono.tenant.domain.entity.Tenant;
import com.chrono.tenant.domain.entity.TenantStatus;
import com.chrono.tenant.domain.repository.TenantRepository;
import com.chrono.tenant.dto.CreateTenantRequest;
import com.chrono.tenant.dto.TenantResponse;
import com.chrono.tenant.dto.UpdateTenantRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    // -------------------------------------------------------------------------
    // createTenant()
    // -------------------------------------------------------------------------

    @Test
    void createTenant_platformAdmin_success() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);

        CreateTenantRequest req = createTenantRequest("Acme Corp", "acme");

        TenantResponse resp = tenantService.createTenant(req, UserRole.PLATFORM_ADMIN.name());

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());

        Tenant saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getSlug()).isEqualTo("acme");
        assertThat(saved.getPlan()).isEqualTo("FREE"); // default
        assertThat(saved.getTimezone()).isEqualTo("UTC"); // default

        assertThat(resp.getSlug()).isEqualTo("acme");
    }

    @Test
    void createTenant_nonPlatformAdmin_throwsBusinessException() {
        CreateTenantRequest req = createTenantRequest("Acme", "acme");

        assertThatThrownBy(() -> tenantService.createTenant(req, UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLATFORM_ADMIN");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenant_duplicateSlug_throwsDuplicateResourceException() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        CreateTenantRequest req = createTenantRequest("Acme Corp", "acme");

        assertThatThrownBy(() -> tenantService.createTenant(req, UserRole.PLATFORM_ADMIN.name()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenant_withExplicitPlan_usesThatPlan() {
        when(tenantRepository.existsBySlug("beta")).thenReturn(false);

        CreateTenantRequest req = createTenantRequest("Beta Ltd", "beta");
        req.setPlan("ENTERPRISE");

        tenantService.createTenant(req, UserRole.PLATFORM_ADMIN.name());

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo("ENTERPRISE");
    }

    // -------------------------------------------------------------------------
    // getTenantById()
    // -------------------------------------------------------------------------

    @Test
    void getTenantById_sameTenant_success() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        TenantResponse resp = tenantService.getTenantById("t-1", "t-1", UserRole.TENANT_ADMIN.name());

        assertThat(resp.getTenantId()).isEqualTo("t-1");
    }

    @Test
    void getTenantById_platformAdmin_bypassesTenantCheck() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        TenantResponse resp = tenantService.getTenantById("t-1", "other-tenant", UserRole.PLATFORM_ADMIN.name());

        assertThat(resp.getTenantId()).isEqualTo("t-1");
    }

    @Test
    void getTenantById_crossTenantAccess_throwsTenantViolation() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() ->
                tenantService.getTenantById("t-1", "t-2", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(TenantViolationException.class);
    }

    @Test
    void getTenantById_softDeleted_throwsResourceNotFoundException() {
        Tenant tenant = validTenant("t-1", "acme");
        tenant.setDeletedAt(LocalDateTime.now().minusHours(1));
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() ->
                tenantService.getTenantById("t-1", "t-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTenantById_unknown_throwsResourceNotFoundException() {
        when(tenantRepository.findByTenantId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tenantService.getTenantById("ghost", "ghost", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // listTenants()
    // -------------------------------------------------------------------------

    @Test
    void listTenants_platformAdmin_returnsPaged() {
        Tenant t1 = validTenant("t-1", "acme");
        Tenant t2 = validTenant("t-2", "beta");
        org.springframework.data.domain.Page<Tenant> page = new PageImpl<>(Arrays.asList(t1, t2));

        when(tenantRepository.searchTenants(isNull(), isNull(), any())).thenReturn(page);

        PageRequest pageReq = new PageRequest();
        pageReq.setPage(0);
        pageReq.setSize(20);

        PageResponse<TenantResponse> resp =
                tenantService.listTenants(null, null, pageReq, UserRole.PLATFORM_ADMIN.name());

        assertThat(resp.getContent()).hasSize(2);
    }

    @Test
    void listTenants_nonPlatformAdmin_throwsBusinessException() {
        PageRequest pageReq = new PageRequest();
        pageReq.setPage(0);
        pageReq.setSize(20);

        assertThatThrownBy(() ->
                tenantService.listTenants(null, null, pageReq, UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLATFORM_ADMIN");
    }

    // -------------------------------------------------------------------------
    // updateTenant()
    // -------------------------------------------------------------------------

    @Test
    void updateTenant_partialUpdate_onlyChangesProvidedFields() {
        Tenant tenant = validTenant("t-1", "acme");
        tenant.setName("Old Name");
        tenant.setMaxUsers(10);
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        UpdateTenantRequest req = new UpdateTenantRequest();
        req.setName("New Name");
        // maxUsers not provided

        TenantResponse resp = tenantService.updateTenant("t-1", req, "t-1", UserRole.PLATFORM_ADMIN.name());

        assertThat(tenant.getName()).isEqualTo("New Name");
        assertThat(tenant.getMaxUsers()).isEqualTo(10); // unchanged
        assertThat(resp.getName()).isEqualTo("New Name");
    }

    @Test
    void updateTenant_statusChangedByNonAdmin_throwsBusinessException() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        UpdateTenantRequest req = new UpdateTenantRequest();
        req.setStatus(TenantStatus.SUSPENDED);

        assertThatThrownBy(() ->
                tenantService.updateTenant("t-1", req, "t-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLATFORM_ADMIN");
    }

    @Test
    void updateTenant_statusChangedByPlatformAdmin_succeeds() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        UpdateTenantRequest req = new UpdateTenantRequest();
        req.setStatus(TenantStatus.SUSPENDED);

        tenantService.updateTenant("t-1", req, "other", UserRole.PLATFORM_ADMIN.name());

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    // -------------------------------------------------------------------------
    // deleteTenant()
    // -------------------------------------------------------------------------

    @Test
    void deleteTenant_platformAdmin_softDeletesTenant() {
        Tenant tenant = validTenant("t-1", "acme");
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant("t-1", UserRole.PLATFORM_ADMIN.name());

        assertThat(tenant.getDeletedAt()).isNotNull();
        assertThat(tenant.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void deleteTenant_nonPlatformAdmin_throwsBusinessException() {
        assertThatThrownBy(() ->
                tenantService.deleteTenant("t-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLATFORM_ADMIN");

        verify(tenantRepository, never()).findByTenantId(any());
    }

    @Test
    void deleteTenant_alreadyDeleted_throwsResourceNotFoundException() {
        Tenant tenant = validTenant("t-1", "acme");
        tenant.setDeletedAt(LocalDateTime.now().minusMinutes(10));
        when(tenantRepository.findByTenantId("t-1")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() ->
                tenantService.deleteTenant("t-1", UserRole.PLATFORM_ADMIN.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Tenant validTenant(String tenantId, String slug) {
        Tenant t = new Tenant();
        t.setTenantId(tenantId);
        t.setName("Tenant " + slug);
        t.setSlug(slug);
        t.setContactEmail("admin@" + slug + ".com");
        t.setPlan("FREE");
        t.setMaxUsers(10);
        t.setTimezone("UTC");
        t.setStatus(TenantStatus.ACTIVE);
        return t;
    }

    private CreateTenantRequest createTenantRequest(String name, String slug) {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setName(name);
        req.setSlug(slug);
        req.setContactEmail("admin@" + slug + ".com");
        req.setMaxUsers(10);
        return req;
    }
}
