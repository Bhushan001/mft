package com.chrono.etl.domain.entity;

import com.chrono.commons.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "etl_jobs",
    schema = "chrono_etl",
    indexes = {
        @Index(name = "idx_etl_tenant_id",    columnList = "tenant_id"),
        @Index(name = "idx_etl_idempotency",  columnList = "tenant_id, source_ref, batch_date", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
public class EtlJob extends BaseEntity {

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /** External reference — e.g. LOS batch ID or file name */
    @Column(name = "source_ref", nullable = false, length = 255)
    private String sourceRef;

    /** Processing date — part of idempotency key */
    @Column(name = "batch_date", nullable = false, length = 10)
    private String batchDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtlJobStatus status;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    /** Spring Batch job instance ID — for restart/resume tracking */
    @Column(name = "batch_job_instance_id")
    private Long batchJobInstanceId;

    /** Spring Batch job execution ID */
    @Column(name = "batch_job_execution_id")
    private Long batchJobExecutionId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
