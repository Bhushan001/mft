package com.chrono.orchestrator.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessRequestDto {
    private String idempotencyKey;
    private String inputPayload;
}
