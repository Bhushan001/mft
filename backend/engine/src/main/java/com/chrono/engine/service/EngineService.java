package com.chrono.engine.service;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.ProcessingStatus;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.engine.client.MapperServiceClient;
import com.chrono.engine.client.dto.MappingRuleDto;
import com.chrono.engine.domain.entity.ProcessingRequest;
import com.chrono.engine.domain.repository.ProcessingRequestRepository;
import com.chrono.engine.dto.ProcessRequest;
import com.chrono.engine.dto.ProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngineService extends BaseService {

    private final ProcessingRequestRepository repository;
    private final MapperServiceClient mapperServiceClient;

    @Transactional
    public ProcessResponse process(ProcessRequest request, String tenantId, String callerRole) {
        // Idempotency check — return cached result for completed requests
        Optional<ProcessingRequest> existing =
            repository.findByTenantIdAndIdempotencyKey(tenantId, request.getIdempotencyKey());

        if (existing.isPresent() && ProcessingStatus.COMPLETED.equals(existing.get().getStatus())) {
            log.info("Idempotent hit: tenantId={}, key={}", tenantId, request.getIdempotencyKey());
            return ProcessResponse.from(existing.get());
        }

        // Create new or reuse failed/pending request
        ProcessingRequest processingRequest = existing.orElseGet(() -> {
            ProcessingRequest pr = new ProcessingRequest();
            pr.setRequestId(UUID.randomUUID().toString());
            pr.setTenantId(tenantId);
            pr.setIdempotencyKey(request.getIdempotencyKey());
            pr.setInputPayload(request.getInputPayload());
            return pr;
        });
        processingRequest.setStatus(ProcessingStatus.IN_PROGRESS);
        repository.save(processingRequest);

        // Fetch active mapping rule
        try {
            ApiResponse<MappingRuleDto> mappingResponse =
                mapperServiceClient.getActiveMappingRule(tenantId);

            if (!mappingResponse.isSuccess() || mappingResponse.getData() == null) {
                throw new BusinessException("No active mapping rule available for tenant: " + tenantId);
            }

            MappingRuleDto rule = mappingResponse.getData();
            processingRequest.setMappingRuleId(rule.getRuleId());

            // Apply mapping — stub for Strategy One integration
            String output = applyMapping(request.getInputPayload(), rule.getRuleDefinition());

            processingRequest.setOutputPayload(output);
            processingRequest.setStatus(ProcessingStatus.COMPLETED);
            processingRequest.setProcessedAt(LocalDateTime.now());

            log.info("Processing completed: requestId={}, tenantId={}, ruleId={}",
                processingRequest.getRequestId(), tenantId, rule.getRuleId());

        } catch (BusinessException e) {
            processingRequest.setStatus(ProcessingStatus.FAILED);
            processingRequest.setErrorMessage(e.getMessage());
            log.error("Processing failed: requestId={}, error={}",
                processingRequest.getRequestId(), e.getMessage());
        } catch (Exception e) {
            processingRequest.setStatus(ProcessingStatus.FAILED);
            processingRequest.setErrorMessage("Unexpected error: " + e.getMessage());
            log.error("Processing failed unexpectedly: requestId={}, error={}",
                processingRequest.getRequestId(), e.getMessage(), e);
        }

        repository.save(processingRequest);
        return ProcessResponse.from(processingRequest);
    }

    @Transactional(readOnly = true)
    public ProcessResponse getRequestById(String requestId, String callerTenantId, String callerRole) {
        ProcessingRequest pr = repository.findByRequestId(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("ProcessingRequest", requestId));
        assertTenantAccess(pr.getTenantId(), callerTenantId, callerRole);
        return ProcessResponse.from(pr);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessResponse> listRequests(
            String tenantId, PageRequest pageRequest, String callerTenantId, String callerRole) {
        assertTenantAccess(tenantId, callerTenantId, callerRole);
        org.springframework.data.domain.Page<ProcessingRequest> page =
            repository.findByTenantIdAndDeletedAtIsNull(tenantId, pageRequest.toSpringPageRequest());
        return PageResponse.from(page.map(ProcessResponse::from));
    }

    /** Stub — real implementation applies LOS-to-Strategy-One field mapping from ruleDefinition */
    private String applyMapping(String inputPayload, String ruleDefinition) {
        // TODO: Implement JSON field mapping using ruleDefinition spec
        return "{\"status\":\"mapped\",\"input\":" + inputPayload + "}";
    }
}
