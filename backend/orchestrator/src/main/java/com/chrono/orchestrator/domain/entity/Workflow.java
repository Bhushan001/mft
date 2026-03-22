package com.chrono.orchestrator.domain.entity;

import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "workflows",
    schema = "chrono_orchestrator",
    indexes = {
        @Index(name = "idx_wf_tenant_id",   columnList = "tenant_id"),
        @Index(name = "idx_wf_correlation", columnList = "correlation_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Workflow extends BaseEntity {

    @Column(name = "workflow_id", nullable = false, unique = true, length = 36)
    private String workflowId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "workflow_type", nullable = false, length = 100)
    private String workflowType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowStep> steps = new ArrayList<>();
}
