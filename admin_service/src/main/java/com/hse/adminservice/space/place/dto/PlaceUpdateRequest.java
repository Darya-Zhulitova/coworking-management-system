package com.hse.adminservice.space.place.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record PlaceUpdateRequest(
        @NotBlank String name,
        BigDecimal locX,
        BigDecimal locY,
        List<String> amenities,
        Boolean active
) {
}
