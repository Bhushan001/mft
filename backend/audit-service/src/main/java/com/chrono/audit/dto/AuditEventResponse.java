package com.chrono.audit.dto;

import com.chrono.audit.domain.entity.AuditEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditEventResponse {

    private String id;
    private String action;
    private String resourceType;
    private String resourceId;
    private String performedBy;
    private String tenantId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditEventResponse from(AuditEvent e) {
        return AuditEventResponse.builder()
                .id(e.getEventId())
                .action(e.getAction())
                .resourceType(e.getResourceType())
                .resourceId(e.getResourceId())
                .performedBy(e.getPerformedBy())
                .tenantId(e.getTenantId())
                .details(e.getDetails())
                .ipAddress(e.getIpAddress())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
