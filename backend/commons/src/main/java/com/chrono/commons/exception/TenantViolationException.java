package com.chrono.commons.exception;

import com.chrono.commons.constants.ErrorCodes;

public class TenantViolationException extends BaseException {

    public TenantViolationException(String message) {
        super(message, ErrorCodes.TENANT_VIOLATION, 403);
    }
}
