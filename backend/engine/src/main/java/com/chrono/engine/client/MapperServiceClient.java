package com.chrono.engine.client;

import com.chrono.commons.dto.ApiResponse;
import com.chrono.engine.client.dto.MappingRuleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "mapper-service", fallback = MapperServiceClientFallback.class)
public interface MapperServiceClient {

    @GetMapping("/api/v1/mappings/active")
    ApiResponse<MappingRuleDto> getActiveMappingRule(@RequestHeader("X-Tenant-Id") String tenantId);
}
