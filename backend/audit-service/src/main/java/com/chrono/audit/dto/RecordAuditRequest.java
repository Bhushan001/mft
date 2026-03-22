package com.chrono.audit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Inbound request from other services (via internal API) to record an audit event.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecordAuditRequest {

    @NotBlank
    @Size(max = 50)
    private String action;

    @NotBlank
    @Size(max = 100)
    private String resourceType;

    @NotBlank
    @Size(max = 255)
    private String resourceId;

    @NotBlank
    @Size(max = 36)
    private String performedBy;

    @NotBlank
    @Size(max = 36)
    private String tenantId;

    private String details;

    @Size(max = 50)
    private String ipAddress;
}
