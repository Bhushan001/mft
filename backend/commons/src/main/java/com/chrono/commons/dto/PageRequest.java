package com.chrono.commons.dto;

import com.chrono.commons.constants.ApiConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Getter
@Setter
@NoArgsConstructor
public class PageRequest {

    @Min(value = 0, message = "Page index must not be negative")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private int size = ApiConstants.DEFAULT_PAGE_SIZE;

    private String sortBy  = ApiConstants.DEFAULT_SORT_BY;
    private String sortDir = ApiConstants.DEFAULT_SORT_DIR;

    public org.springframework.data.domain.PageRequest toSpringPageRequest() {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
