package com.hse.adminservice.auth.service;

import com.hse.adminservice.auth.dto.AuthRequest;
import com.hse.adminservice.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
}
