package com.chrono.engine.dto;

import com.chrono.engine.domain.entity.ProcessingRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProcessResponse {

    private String requestId;
    private String tenantId;
    private String idempotencyKey;
    private String status;
    private String outputPayload;
    private String errorMessage;
    private LocalDateTime processedAt;

    public static ProcessResponse from(ProcessingRequest pr) {
        ProcessResponse response = new ProcessResponse();
        response.setRequestId(pr.getRequestId());
        response.setTenantId(pr.getTenantId());
        response.setIdempotencyKey(pr.getIdempotencyKey());
        response.setStatus(pr.getStatus() != null ? pr.getStatus().name() : null);
        response.setOutputPayload(pr.getOutputPayload());
        response.setErrorMessage(pr.getErrorMessage());
        response.setProcessedAt(pr.getProcessedAt());
        return response;
    }
}
