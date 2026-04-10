package com.hse.adminservice.service;

import com.hse.adminservice.dto.AppContextResponse;

public interface ContextService {
    AppContextResponse getContext(Long coworkingId);
}
