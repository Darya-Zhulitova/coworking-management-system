package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record CoworkingJoinLinkResponse(
        Long coworkingId,
        String joinToken,
        String joinUrl
) {
}
