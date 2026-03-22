package com.chrono.user.client;

import com.chrono.user.client.dto.RegisterCredentialRequest;
import com.chrono.commons.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", path = "/api/v1/auth/internal")
public interface AuthServiceClient {

    @PostMapping("/register")
    ApiResponse<Void> registerCredential(@RequestBody RegisterCredentialRequest request);

    @DeleteMapping("/credentials/{userId}")
    ApiResponse<Void> deleteCredential(@PathVariable("userId") String userId);
}
