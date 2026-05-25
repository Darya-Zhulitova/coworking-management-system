package com.hse.adminservice.space.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record PlaceUpdateRequest(
        @NotBlank String name,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal locX,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal locY,
        String imageFileId,
        List<String> amenities,
        Boolean active
) {
}
