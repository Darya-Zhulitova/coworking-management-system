package com.hse.adminservice.configlookup.dto;

import lombok.Builder;

@Builder
public record ServiceRequestTypeLookupResponse(
        Long id,
        String name,
        Long cost,
        Integer version,
        Boolean active
) {
}
