package com.chrono.commons.constants;

public final class ErrorCodes {

    private ErrorCodes() {}

    public static final String RESOURCE_NOT_FOUND = "ERR_001";
    public static final String VALIDATION_FAILED  = "ERR_002";
    public static final String UNAUTHORIZED       = "ERR_003";
    public static final String FORBIDDEN          = "ERR_004";
    public static final String TENANT_VIOLATION   = "ERR_005";
    public static final String DUPLICATE_RESOURCE = "ERR_006";
    public static final String BUSINESS_RULE      = "ERR_007";
    public static final String INTERNAL_ERROR     = "ERR_099";
}
