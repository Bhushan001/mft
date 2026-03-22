package com.chrono.commons.exception;

import com.chrono.commons.constants.ErrorCodes;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(
            String.format("%s already exists with %s: %s", resource, field, value),
            ErrorCodes.DUPLICATE_RESOURCE,
            409
        );
    }
}
