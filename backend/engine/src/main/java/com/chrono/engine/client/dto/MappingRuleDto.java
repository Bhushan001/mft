package com.chrono.engine.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MappingRuleDto {

    private String ruleId;
    private String tenantId;
    private String name;
    private int version;
    private boolean active;
    private String ruleDefinition;
    private String sourceSystem;
    private String targetSystem;
}
