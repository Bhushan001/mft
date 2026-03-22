package com.chrono.commons.constants;

public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_V1 = "/api/v1";

    public static final String HEADER_TENANT_ID       = "X-Tenant-Id";
    public static final String HEADER_USER_ID         = "X-User-Id";
    public static final String HEADER_USER_ROLE       = "X-User-Role";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final int    DEFAULT_PAGE_SIZE = 20;
    public static final int    MAX_PAGE_SIZE     = 100;
    public static final String DEFAULT_SORT_BY   = "createdAt";
    public static final String DEFAULT_SORT_DIR  = "desc";
}
