package com.chrono.commons.web;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    protected ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.ok(ApiResponse.<Void>noContent());
    }

    protected <T> ResponseEntity<ApiResponse<PageResponse<T>>> page(PageResponse<T> pageResponse) {
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
}
