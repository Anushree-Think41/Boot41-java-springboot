package com.authboilerplate.controller;

import com.authboilerplate.dto.ApiResponse;
import com.authboilerplate.dto.AuthResponse;
import com.authboilerplate.dto.RegisterRequest;
import com.authboilerplate.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Public endpoint — register a new user.
     * Body: { "username": "...", "email": "...", "password": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for username: {}", request.getUsername());
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(authResponse)
                        .build());
    }

    /**
     * Protected endpoint — requires HTTP Basic Auth header.
     * Header: Authorization: Basic base64(username:password)
     * Returns the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Profile request for authenticated user: {}", userDetails.getUsername());
        AuthResponse authResponse = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Authenticated successfully")
                .data(authResponse)
                .build());
    }
}
