package com.chrono.etl.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class SubmitEtlJobRequest {

    @NotBlank(message = "sourceRef is required")
    private String sourceRef;

    @NotBlank(message = "batchDate is required")
    private String batchDate;

    private String inputPayload;
}
