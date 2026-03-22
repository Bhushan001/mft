package com.chrono.tenant.dto;

import com.chrono.tenant.domain.entity.Tenant;
import com.chrono.tenant.domain.entity.TenantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TenantResponse {

    private Long id;
    private String tenantId;
    private String name;
    private String slug;
    private TenantStatus status;
    private String plan;
    private String contactEmail;
    private int maxUsers;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TenantResponse from(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .plan(tenant.getPlan())
                .contactEmail(tenant.getContactEmail())
                .maxUsers(tenant.getMaxUsers())
                .timezone(tenant.getTimezone())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
