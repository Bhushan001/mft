package com.chrono.etl.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessResponseDto {
    private String requestId;
    private String status;
    private String outputPayload;
    private String errorMessage;
}
