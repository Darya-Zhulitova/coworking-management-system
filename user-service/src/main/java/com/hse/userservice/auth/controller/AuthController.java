package com.hse.userservice.auth.controller;

import com.hse.userservice.auth.dto.AuthResponse;
import com.hse.userservice.auth.dto.LoginRequest;
import com.hse.userservice.auth.dto.RegisterRequest;
import com.hse.userservice.auth.service.AuthService;
import com.hse.userservice.dto.response.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public UserProfileDto getCurrentUserProfile() {
        return authService.getCurrentUserProfile();
    }
}
