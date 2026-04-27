package com.hse.adminservice.admincontext.application;

import com.hse.adminservice.admincontext.dto.AppContextResponse;

public interface ContextService {
    AppContextResponse getContext(Long coworkingId);
}
