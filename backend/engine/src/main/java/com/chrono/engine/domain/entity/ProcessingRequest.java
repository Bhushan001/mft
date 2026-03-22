package com.chrono.engine.domain.entity;

import com.chrono.commons.enums.ProcessingStatus;
import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "processing_requests",
    schema = "chrono_engine",
    indexes = {
        @Index(name = "idx_pr_tenant_id",    columnList = "tenant_id"),
        @Index(name = "idx_pr_idempotency",  columnList = "tenant_id, idempotency_key", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ProcessingRequest extends BaseEntity {

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus status;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "mapping_rule_id", length = 36)
    private String mappingRuleId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
