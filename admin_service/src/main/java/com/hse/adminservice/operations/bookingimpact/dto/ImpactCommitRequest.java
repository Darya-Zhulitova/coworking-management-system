package com.hse.adminservice.operations.bookingimpact.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImpactCommitRequest {
    @NotBlank
    private String impactHash;
}
