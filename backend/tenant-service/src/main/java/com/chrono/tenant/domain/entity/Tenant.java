package com.chrono.tenant.domain.entity;

import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(
    name = "tenants",
    schema = "chrono_tenant",
    indexes = {
        @Index(name = "idx_tenant_id",   columnList = "tenant_id",  unique = true),
        @Index(name = "idx_tenant_slug", columnList = "slug",       unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(nullable = false, length = 30)
    private String plan = "FREE";

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "max_users", nullable = false)
    private int maxUsers = 10;

    @Column(length = 50)
    private String timezone = "UTC";

    /** JSON blob for arbitrary per-tenant settings (e.g. branding, feature flags) */
    @Column(columnDefinition = "TEXT")
    private String settings;
}
