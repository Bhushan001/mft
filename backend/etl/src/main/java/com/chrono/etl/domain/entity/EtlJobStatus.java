package com.chrono.etl.domain.entity;

public enum EtlJobStatus {
    SUBMITTED,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED   // idempotent skip — same key already COMPLETED
}
