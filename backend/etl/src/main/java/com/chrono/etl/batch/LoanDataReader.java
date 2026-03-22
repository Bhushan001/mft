package com.chrono.etl.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.Iterator;
import java.util.List;

/**
 * Reads loan records from the source system (LOS).
 * Stub implementation — real implementation fetches from LOS API or file.
 */
@Slf4j
public class LoanDataReader implements ItemReader<LoanRecordDto> {

    private final String tenantId;
    private final String jobId;
    private final String inputPayload;
    private final Iterator<LoanRecordDto> iterator;

    public LoanDataReader(String tenantId, String jobId, String inputPayload) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.inputPayload = inputPayload;
        // TODO: Replace stub with real LOS data extraction
        this.iterator = buildStubRecords().iterator();
    }

    @Override
    public LoanRecordDto read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<LoanRecordDto> buildStubRecords() {
        LoanRecordDto record = new LoanRecordDto();
        record.setRecordId(jobId + "-001");
        record.setTenantId(tenantId);
        record.setJobId(jobId);
        record.setRawData(inputPayload != null ? inputPayload : "{}");
        return java.util.Collections.singletonList(record);
    }
}
