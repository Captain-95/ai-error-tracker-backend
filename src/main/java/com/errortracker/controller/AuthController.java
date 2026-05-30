package com.errortracker.controller;

import com.errortracker.dto.request.LoginRequest;
import com.errortracker.dto.request.RegisterRequest;
import com.errortracker.dto.response.ApiResponse;
import com.errortracker.dto.response.AuthResponse;
import com.errortracker.ratelimit.RateLimit;
import com.errortracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(
            key = "auth:register",
            limit = 5,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.IP
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @RateLimit(
            key = "auth:login",
            limit = 10,
            windowSeconds = 60,
            keySource = RateLimit.KeySource.IP
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}