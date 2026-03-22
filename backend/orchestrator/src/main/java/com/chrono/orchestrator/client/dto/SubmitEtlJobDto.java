package com.chrono.orchestrator.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitEtlJobDto {
    private String sourceRef;
    private String batchDate;
    private String inputPayload;
}
