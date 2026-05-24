package com.hse.userservice.internal.dto;

import java.util.List;

public record InternalServiceRequestWorkspaceDto(
        InternalServiceRequestDetailDto request,
        List<InternalServiceRequestMessageDto> messages,
        List<String> availableActions
) {
}
