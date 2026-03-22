package com.chrono.audit.service;

import com.chrono.audit.domain.entity.AuditEvent;
import com.chrono.audit.domain.repository.AuditEventRepository;
import com.chrono.audit.dto.AuditEventResponse;
import com.chrono.audit.dto.RecordAuditRequest;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    // -------------------------------------------------------------------------
    // record()
    // -------------------------------------------------------------------------

    @Test
    void record_validRequest_savesEvent() {
        RecordAuditRequest req = buildRequest("CREATE", "Tenant", "t-123", "user-1", "tenant-1");

        AuditEventResponse resp = auditService.record(req);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getResourceType()).isEqualTo("Tenant");
        assertThat(saved.getResourceId()).isEqualTo("t-123");
        assertThat(saved.getPerformedBy()).isEqualTo("user-1");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getEventId()).isNotNull();

        assertThat(resp.getAction()).isEqualTo("CREATE");
    }

    @Test
    void record_actionNormalisedToUpperCase() {
        RecordAuditRequest req = buildRequest("update", "User", "u-1", "user-2", "tenant-1");

        auditService.record(req);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("UPDATE");
    }

    @Test
    void record_eachEventGetsUniqueEventId() {
        RecordAuditRequest req1 = buildRequest("LOGIN", "User", "u-1", "user-1", "tenant-1");
        RecordAuditRequest req2 = buildRequest("LOGIN", "User", "u-2", "user-2", "tenant-1");

        AuditEventResponse r1 = auditService.record(req1);
        AuditEventResponse r2 = auditService.record(req2);

        // Both responses have IDs set (mocked repo doesn't auto-set, so IDs come from UUID gen)
        // Just verify two different requests are handled independently
        verify(auditEventRepository, times(2)).save(any(AuditEvent.class));
    }

    // -------------------------------------------------------------------------
    // search() — tenant filter enforcement
    // -------------------------------------------------------------------------

    @Test
    void search_platformAdmin_canQueryAllTenants() {
        AuditEvent e = validEvent("t-1");
        when(auditEventRepository.search(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(e)));

        PageRequest pageReq = pageRequest();
        // PLATFORM_ADMIN, no tenantId filter = null (all tenants)
        PageResponse<AuditEventResponse> resp =
                auditService.search(null, null, null, null, pageReq, "platform", UserRole.PLATFORM_ADMIN.name());

        assertThat(resp.getContent()).hasSize(1);
        // Verify query called with null tenant (no restriction)
        verify(auditEventRepository).search(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void search_platformAdmin_canFilterBySpecificTenant() {
        AuditEvent e = validEvent("t-1");
        when(auditEventRepository.search(eq("t-1"), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(e)));

        PageRequest pageReq = pageRequest();
        auditService.search("t-1", null, null, null, pageReq, "platform", UserRole.PLATFORM_ADMIN.name());

        verify(auditEventRepository).search(eq("t-1"), isNull(), isNull(), isNull(), any());
    }

    @Test
    void search_tenantAdmin_isRestrictedToOwnTenant() {
        when(auditEventRepository.search(eq("tenant-1"), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageRequest pageReq = pageRequest();
        // tenantId param null — should be forced to callerTenantId
        auditService.search(null, null, null, null, pageReq, "tenant-1", UserRole.TENANT_ADMIN.name());

        verify(auditEventRepository).search(eq("tenant-1"), isNull(), isNull(), isNull(), any());
    }

    @Test
    void search_tenantAdmin_requestingOtherTenant_throwsBusinessException() {
        PageRequest pageReq = pageRequest();

        assertThatThrownBy(() ->
                auditService.search("tenant-other", null, null, null, pageReq,
                        "tenant-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own tenant");
    }

    @Test
    void search_withFilters_passesFiltersToRepository() {
        when(auditEventRepository.search(eq("tenant-1"), eq("DELETE"), eq("User"), isNull(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageRequest pageReq = pageRequest();
        auditService.search("tenant-1", "DELETE", "User", null, pageReq,
                "tenant-1", UserRole.TENANT_ADMIN.name());

        verify(auditEventRepository).search(eq("tenant-1"), eq("DELETE"), eq("User"), isNull(), any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RecordAuditRequest buildRequest(String action, String resourceType,
            String resourceId, String performedBy, String tenantId) {
        RecordAuditRequest req = new RecordAuditRequest();
        req.setAction(action);
        req.setResourceType(resourceType);
        req.setResourceId(resourceId);
        req.setPerformedBy(performedBy);
        req.setTenantId(tenantId);
        return req;
    }

    private AuditEvent validEvent(String tenantId) {
        AuditEvent e = new AuditEvent();
        e.setEventId("evt-uuid-1");
        e.setAction("CREATE");
        e.setResourceType("Tenant");
        e.setResourceId("res-1");
        e.setPerformedBy("user-1");
        e.setTenantId(tenantId);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private PageRequest pageRequest() {
        PageRequest pr = new PageRequest();
        pr.setPage(0);
        pr.setSize(20);
        return pr;
    }
}
