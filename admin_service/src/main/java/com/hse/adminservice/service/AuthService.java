package com.hse.adminservice.service;

import com.hse.adminservice.dto.AuthRequest;
import com.hse.adminservice.dto.AuthResponse;

public interface AuthService {

    AuthResponse login(AuthRequest request);
}
