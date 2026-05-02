package com.hse.userservice.dto.response;

public record ServiceRequestTypeOptionDto(
        Long id,
        Long coworkingId,
        String name,
        Integer cost
) {
}
