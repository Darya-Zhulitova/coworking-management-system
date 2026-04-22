package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record CoworkingListItemResponse(
        Long id,
        String name,
        String role,
        Boolean active,
        Boolean archived
) {
}
