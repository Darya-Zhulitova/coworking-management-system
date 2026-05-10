package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;

public record ResolvedPlace(
        CoworkingConfigSnapshot.Place place,
        CoworkingConfigSnapshot.PlaceType placeType,
        CoworkingConfigSnapshot.Tariff tariff,
        CoworkingConfigSnapshot.Floor floor,
        Long coworkingId
) {
}
