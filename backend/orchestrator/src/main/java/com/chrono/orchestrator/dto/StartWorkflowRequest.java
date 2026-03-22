package com.chrono.orchestrator.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class StartWorkflowRequest {

    @NotBlank
    private String workflowType;

    private String correlationId;

    @NotBlank
    private String inputPayload;
}
