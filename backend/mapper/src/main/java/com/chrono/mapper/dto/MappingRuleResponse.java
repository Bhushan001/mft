package com.chrono.mapper.dto;

import com.chrono.mapper.domain.entity.MappingRule;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MappingRuleResponse {

    private String ruleId;
    private String tenantId;
    private String name;
    private String description;
    private int version;
    private boolean active;
    private String ruleDefinition;
    private String sourceSystem;
    private String targetSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MappingRuleResponse from(MappingRule rule) {
        MappingRuleResponse response = new MappingRuleResponse();
        response.setRuleId(rule.getRuleId());
        response.setTenantId(rule.getTenantId());
        response.setName(rule.getName());
        response.setDescription(rule.getDescription());
        response.setVersion(rule.getVersion());
        response.setActive(rule.isActive());
        response.setRuleDefinition(rule.getRuleDefinition());
        response.setSourceSystem(rule.getSourceSystem());
        response.setTargetSystem(rule.getTargetSystem());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}
