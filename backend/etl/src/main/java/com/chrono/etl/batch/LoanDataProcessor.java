package com.chrono.etl.batch;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.etl.client.EngineServiceClient;
import com.chrono.etl.client.dto.ProcessRequestDto;
import com.chrono.etl.client.dto.ProcessResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * Transforms each loan record by calling the Engine Service.
 * Circuit breaker on EngineServiceClient handles Engine unavailability.
 */
@Slf4j
@RequiredArgsConstructor
public class LoanDataProcessor implements ItemProcessor<LoanRecordDto, LoanRecordDto> {

    private final EngineServiceClient engineServiceClient;
    private final String callerRole;

    @Override
    public LoanRecordDto process(LoanRecordDto item) throws Exception {
        ProcessRequestDto request = new ProcessRequestDto();
        request.setIdempotencyKey(item.getJobId() + "-" + item.getRecordId());
        request.setInputPayload(item.getRawData());

        ApiResponse<ProcessResponseDto> response =
            engineServiceClient.process(item.getTenantId(), callerRole, request);

        if (response.isSuccess() && response.getData() != null) {
            item.setTransformedData(response.getData().getOutputPayload());
            log.debug("Record transformed: recordId={}", item.getRecordId());
        } else {
            log.warn("Engine processing failed for recordId={}: {}", item.getRecordId(), response.getMessage());
            item.setTransformedData(item.getRawData()); // pass-through on failure
        }

        return item;
    }
}
