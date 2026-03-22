package com.chrono.etl.batch;

import com.chrono.etl.client.EngineServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class LoanProcessingJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EngineServiceClient engineServiceClient;

    public static final String JOB_NAME = "loanProcessingJob";

    @Bean(JOB_NAME)
    public Job loanProcessingJob() {
        return jobBuilderFactory.get(JOB_NAME)
            .incrementer(new RunIdIncrementer())
            .start(loanProcessingStep(null, null, null))
            .build();
    }

    @Bean
    @JobScope
    public Step loanProcessingStep(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['jobId']}") String jobId,
            @Value("#{jobParameters['inputPayload']}") String inputPayload) {

        return stepBuilderFactory.get("loanProcessingStep")
            .<LoanRecordDto, LoanRecordDto>chunk(10)
            .reader(new LoanDataReader(tenantId, jobId, inputPayload))
            .processor(new LoanDataProcessor(engineServiceClient, "SYSTEM"))
            .writer(new LoanDataWriter())
            .build();
    }
}
