package com.chrono.etl.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * Loads transformed loan records into Strategy One (target system).
 * Stub implementation — real implementation calls Strategy One API.
 */
@Slf4j
public class LoanDataWriter implements ItemWriter<LoanRecordDto> {

    @Override
    public void write(List<? extends LoanRecordDto> items) throws Exception {
        for (LoanRecordDto item : items) {
            // TODO: Implement actual Strategy One API call
            log.info("Writing record to Strategy One: recordId={}, tenantId={}",
                item.getRecordId(), item.getTenantId());
        }
    }
}
