package com.chrono.etl.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Overrides default BatchConfigurer to ensure Spring Batch metadata tables
 * are created in the chrono_etl schema (configured via datasource URL).
 * table-prefix is set via application config: spring.batch.schema=BATCH_
 */
@Configuration
public class BatchConfig extends DefaultBatchConfigurer {

    public BatchConfig(DataSource dataSource) {
        super(dataSource);
    }
}
