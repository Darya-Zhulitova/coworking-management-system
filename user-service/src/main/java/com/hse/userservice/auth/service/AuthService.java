package com.hse.userservice.auth.service;

import com.hse.userservice.auth.dto.AuthResponse;
import com.hse.userservice.auth.dto.LoginRequest;
import com.hse.userservice.auth.dto.RegisterRequest;
import com.hse.userservice.dto.response.UserProfileDto;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    UserProfileDto getCurrentUserProfile();
}
