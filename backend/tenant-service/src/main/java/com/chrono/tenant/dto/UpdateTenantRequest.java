package com.chrono.tenant.dto;

import com.chrono.tenant.domain.entity.TenantStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;

@Getter
@NoArgsConstructor
public class UpdateTenantRequest {

    @Size(max = 150)
    private String name;

    @Email
    private String contactEmail;

    private TenantStatus status;

    @Size(max = 30)
    private String plan;

    @Min(1) @Max(10000)
    private Integer maxUsers;

    @Size(max = 50)
    private String timezone;
}
