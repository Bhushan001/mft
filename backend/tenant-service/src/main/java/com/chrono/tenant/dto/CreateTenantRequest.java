package com.chrono.tenant.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;

@Getter
@NoArgsConstructor
public class CreateTenantRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    /** URL-safe identifier, e.g. "acme-corp" */
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens only")
    private String slug;

    @NotBlank
    @Email
    private String contactEmail;

    @Size(max = 30)
    private String plan;

    @Min(1) @Max(10000)
    private int maxUsers = 10;

    @Size(max = 50)
    private String timezone;
}
