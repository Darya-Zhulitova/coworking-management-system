package com.hse.adminservice.operations.servicedesk.dto;

import java.util.List;

public record ServiceRequestWorkspaceResponse(
        ServiceRequestDetailResponse request,
        List<ServiceRequestMessageResponse> messages,
        List<String> availableActions
) {
}
