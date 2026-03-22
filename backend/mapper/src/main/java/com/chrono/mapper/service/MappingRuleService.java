package com.chrono.mapper.service;

import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.mapper.domain.entity.MappingRule;
import com.chrono.mapper.domain.repository.MappingRuleRepository;
import com.chrono.mapper.dto.CreateMappingRuleRequest;
import com.chrono.mapper.dto.MappingRuleResponse;
import com.chrono.mapper.dto.UpdateMappingRuleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingRuleService extends BaseService {

    private final MappingRuleRepository mappingRuleRepository;

    @Transactional
    public MappingRuleResponse createMappingRule(CreateMappingRuleRequest request, String tenantId) {
        MappingRule rule = new MappingRule();
        rule.setRuleId(UUID.randomUUID().toString());
        rule.setTenantId(tenantId);
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setRuleDefinition(request.getRuleDefinition());
        rule.setSourceSystem(request.getSourceSystem());
        rule.setTargetSystem(request.getTargetSystem());
        rule.setVersion(1);
        rule.setActive(false);

        mappingRuleRepository.save(rule);
        log.info("Mapping rule created: ruleId={}, tenantId={}", rule.getRuleId(), tenantId);
        return MappingRuleResponse.from(rule);
    }

    @Cacheable(value = "mappingRules", key = "#tenantId")
    @Transactional(readOnly = true)
    public MappingRuleResponse getActiveMappingRule(String tenantId) {
        MappingRule rule = mappingRuleRepository.findByTenantIdAndActiveTrue(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Active MappingRule for tenant", tenantId));
        return MappingRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public MappingRuleResponse getMappingRuleById(String ruleId, String callerTenantId, String callerRole) {
        MappingRule rule = findActiveRule(ruleId);
        assertTenantAccess(rule.getTenantId(), callerTenantId, callerRole);
        return MappingRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public PageResponse<MappingRuleResponse> listByTenant(
            String tenantId, String name, PageRequest pageRequest,
            String callerTenantId, String callerRole) {
        assertTenantAccess(tenantId, callerTenantId, callerRole);
        org.springframework.data.domain.Page<MappingRule> page =
            mappingRuleRepository.searchByTenant(tenantId, name, pageRequest.toSpringPageRequest());
        return PageResponse.from(page.map(MappingRuleResponse::from));
    }

    @CacheEvict(value = "mappingRules", allEntries = true)
    @Transactional
    public MappingRuleResponse updateMappingRule(
            String ruleId, UpdateMappingRuleRequest request,
            String callerTenantId, String callerRole) {
        MappingRule rule = findActiveRule(ruleId);
        assertTenantAccess(rule.getTenantId(), callerTenantId, callerRole);

        if (rule.isActive()) {
            throw new BusinessException("Cannot update an active mapping rule. Deactivate it first or create a new version.");
        }

        if (request.getName() != null)          rule.setName(request.getName());
        if (request.getDescription() != null)   rule.setDescription(request.getDescription());
        if (request.getRuleDefinition() != null) rule.setRuleDefinition(request.getRuleDefinition());
        if (request.getSourceSystem() != null)  rule.setSourceSystem(request.getSourceSystem());
        if (request.getTargetSystem() != null)  rule.setTargetSystem(request.getTargetSystem());

        log.info("Mapping rule updated: ruleId={}", ruleId);
        return MappingRuleResponse.from(rule);
    }

    @CacheEvict(value = "mappingRules", allEntries = true)
    @Transactional
    public MappingRuleResponse publishMappingRule(String ruleId, String callerTenantId, String callerRole) {
        MappingRule rule = findActiveRule(ruleId);
        assertTenantAccess(rule.getTenantId(), callerTenantId, callerRole);

        // Deactivate current active rule for this tenant
        mappingRuleRepository.findByTenantIdAndActiveTrue(rule.getTenantId())
            .ifPresent(current -> {
                current.setActive(false);
                mappingRuleRepository.save(current);
            });

        // Activate and increment version
        rule.setActive(true);
        rule.setVersion(rule.getVersion() + 1);
        mappingRuleRepository.save(rule);

        log.info("Mapping rule published: ruleId={}, tenantId={}, version={}",
            rule.getRuleId(), rule.getTenantId(), rule.getVersion());
        return MappingRuleResponse.from(rule);
    }

    @CacheEvict(value = "mappingRules", allEntries = true)
    @Transactional
    public void deleteMappingRule(String ruleId, String callerTenantId, String callerRole) {
        MappingRule rule = findActiveRule(ruleId);
        assertTenantAccess(rule.getTenantId(), callerTenantId, callerRole);

        if (rule.isActive()) {
            throw new BusinessException("Cannot delete an active mapping rule. Publish another rule to replace it first.");
        }

        rule.setDeletedAt(LocalDateTime.now());
        log.info("Mapping rule soft-deleted: ruleId={}", ruleId);
    }

    private MappingRule findActiveRule(String ruleId) {
        return mappingRuleRepository.findByRuleId(ruleId)
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("MappingRule", ruleId));
    }
}
