package com.chrono.commons.exception;

import com.chrono.commons.constants.ErrorCodes;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(
            String.format("%s not found with identifier: %s", resource, identifier),
            ErrorCodes.RESOURCE_NOT_FOUND,
            404
        );
    }
}
