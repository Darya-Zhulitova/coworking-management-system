package com.hse.adminservice.coworking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CoworkingCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 255) String workingHoursLabel,
        @Size(max = 255) String heroTitle,
        @Size(max = 2000) String heroText,
        List<@NotBlank @Size(max = 2000) String> imageUrls,
        Boolean autoApproveMembership
) {
}
