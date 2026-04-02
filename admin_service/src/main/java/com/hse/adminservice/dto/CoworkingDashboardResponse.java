package com.hse.adminservice.dto;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import lombok.Builder;

import java.util.Set;

@Builder
public record CoworkingDashboardResponse(
        CoworkingResponse coworking,
        Set<EffectiveAdminAction> grantedActions,
        String subjectLabel
) {
}
