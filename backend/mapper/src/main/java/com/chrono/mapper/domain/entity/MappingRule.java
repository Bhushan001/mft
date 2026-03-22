package com.chrono.mapper.domain.entity;

import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(
    name = "mapping_rules",
    schema = "chrono_mapper",
    indexes = {
        @Index(name = "idx_mr_tenant_id",     columnList = "tenant_id"),
        @Index(name = "idx_mr_tenant_active", columnList = "tenant_id, active")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MappingRule extends BaseEntity {

    @Column(name = "rule_id", nullable = false, unique = true, length = 36)
    private String ruleId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "rule_definition", columnDefinition = "TEXT")
    private String ruleDefinition;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;
}
