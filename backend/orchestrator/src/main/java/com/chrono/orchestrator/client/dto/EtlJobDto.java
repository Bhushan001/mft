package com.chrono.orchestrator.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtlJobDto {
    private String jobId;
    private String status;
    private String errorMessage;
}
