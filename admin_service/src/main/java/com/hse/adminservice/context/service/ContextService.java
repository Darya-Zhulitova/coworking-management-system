package com.hse.adminservice.context.service;

import com.hse.adminservice.context.dto.AppContextResponse;

public interface ContextService {
    AppContextResponse getContext(Long coworkingId);
}
