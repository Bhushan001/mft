package com.chrono.mapper.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class CreateMappingRuleRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String ruleDefinition;

    private String sourceSystem;

    private String targetSystem;
}
