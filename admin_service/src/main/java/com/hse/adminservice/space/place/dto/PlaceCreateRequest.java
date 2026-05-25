package com.hse.adminservice.space.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PlaceCreateRequest(
        @NotBlank String name,
        @NotNull Long floorId,
        @NotNull Long placeTypeId,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal locX,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal locY,
        String imageFileId,
        List<String> amenities
) {
}
