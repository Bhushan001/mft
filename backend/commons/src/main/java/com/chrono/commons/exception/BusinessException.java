package com.chrono.commons.exception;

import com.chrono.commons.constants.ErrorCodes;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, ErrorCodes.BUSINESS_RULE, 422);
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode, 422);
    }
}
