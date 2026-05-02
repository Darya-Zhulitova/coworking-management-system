package com.hse.userservice.client;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.client.dto.CoworkingInfo;

public interface AdminServiceClient {
    CoworkingInfo getCoworkingInfo(Long coworkingId);

    CoworkingConfigSnapshot getCoworkingConfigSnapshot(Long coworkingId);
}
