package com.chrono.audit.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "audit_events",
    schema = "chrono_audit",
    indexes = {
        @Index(name = "idx_ae_tenant_id",    columnList = "tenant_id"),
        @Index(name = "idx_ae_action",       columnList = "action"),
        @Index(name = "idx_ae_resource_type", columnList = "resource_type"),
        @Index(name = "idx_ae_performed_by", columnList = "performed_by"),
        @Index(name = "idx_ae_created_at",   columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for public-facing identity — never expose the surrogate PK */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    /** e.g. CREATE, UPDATE, DELETE, LOGIN, LOGOUT */
    @Column(nullable = false, length = 50)
    private String action;

    /** Domain type that was acted on — e.g. Tenant, User, MappingRule */
    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    /** UUID / identifier of the acted-upon resource */
    @Column(name = "resource_id", nullable = false, length = 255)
    private String resourceId;

    /** userId of the actor */
    @Column(name = "performed_by", nullable = false, length = 36)
    private String performedBy;

    /** Tenant context under which the action was performed */
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /** Optional JSON or human-readable detail (diff, old/new value) */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
