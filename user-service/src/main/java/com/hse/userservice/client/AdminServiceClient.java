package com.hse.userservice.client;

import com.hse.userservice.client.dto.CoworkingInfo;

public interface AdminServiceClient {

    CoworkingInfo getCoworkingById(Long coworkingId);
}