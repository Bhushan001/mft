package com.chrono.etl.batch;

import lombok.Getter;
import lombok.Setter;

/** Intermediate data model passed between Batch Reader → Processor → Writer */
@Getter
@Setter
public class LoanRecordDto {

    private String recordId;
    private String tenantId;
    private String jobId;
    private String rawData;
    private String transformedData;
}
