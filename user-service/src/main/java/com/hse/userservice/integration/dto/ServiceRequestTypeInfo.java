package com.hse.userservice.integration.dto;

public record ServiceRequestTypeInfo(
        Long id,
        String name,
        Long cost,
        Integer version,
        Boolean active
) {
}
