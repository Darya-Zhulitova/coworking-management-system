package com.hse.adminservice.coworking.dto;

import com.hse.adminservice.rbac.domain.Grant;
import lombok.Builder;

import java.util.Set;

@Builder
public record CoworkingDashboardResponse(
        CoworkingResponse coworking,
        Set<Grant> grants,
        String subjectLabel
) {
}
