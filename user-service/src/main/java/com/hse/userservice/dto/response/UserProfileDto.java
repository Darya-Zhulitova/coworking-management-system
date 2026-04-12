package com.hse.userservice.dto.response;

public record UserProfileDto(
        Long id,
        String email,
        String name,
        String description
) {
}
