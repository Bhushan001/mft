package com.chrono.orchestrator.domain.entity;

import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "workflow_steps",
    schema = "chrono_orchestrator",
    indexes = {
        @Index(name = "idx_ws_workflow_id", columnList = "workflow_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class WorkflowStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "compensation_payload", columnDefinition = "TEXT")
    private String compensationPayload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
