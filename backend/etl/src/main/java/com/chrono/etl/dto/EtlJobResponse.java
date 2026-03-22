package com.chrono.etl.dto;

import com.chrono.etl.domain.entity.EtlJob;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EtlJobResponse {

    private String jobId;
    private String tenantId;
    private String sourceRef;
    private String batchDate;
    private String status;
    private String errorMessage;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    public static EtlJobResponse from(EtlJob job) {
        EtlJobResponse response = new EtlJobResponse();
        response.setJobId(job.getJobId());
        response.setTenantId(job.getTenantId());
        response.setSourceRef(job.getSourceRef());
        response.setBatchDate(job.getBatchDate());
        response.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
        response.setErrorMessage(job.getErrorMessage());
        response.setSubmittedAt(job.getSubmittedAt());
        response.setCompletedAt(job.getCompletedAt());
        return response;
    }
}
