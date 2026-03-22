package com.chrono.mapper.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMappingRuleRequest {

    private String name;
    private String description;
    private String ruleDefinition;
    private String sourceSystem;
    private String targetSystem;
}
