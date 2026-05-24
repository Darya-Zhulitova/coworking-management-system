package com.hse.userservice.feature.user.controller;

import com.hse.userservice.feature.user.dto.*;
import com.hse.userservice.feature.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUserProfile() {
        return userService.getCurrentUserProfile();
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUserProfile(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateCurrentUserProfile(request);
    }
}
