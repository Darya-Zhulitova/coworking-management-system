package com.hse.adminservice.auth.application;

import com.hse.adminservice.auth.dto.AuthRequest;
import com.hse.adminservice.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
}
