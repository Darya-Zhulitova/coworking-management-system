package com.hse.userservice.client;

import com.hse.userservice.client.dto.CoworkingInfo;
import org.springframework.stereotype.Component;

@Component
public class AdminServiceClientStub implements AdminServiceClient {

    @Override
    public CoworkingInfo getCoworkingById(Long coworkingId) {
        if (coworkingId == null || coworkingId <= 0) {
            return null;
        }

        return new CoworkingInfo(
                coworkingId,
                "Coworking #" + coworkingId,
                true
        );
    }
}