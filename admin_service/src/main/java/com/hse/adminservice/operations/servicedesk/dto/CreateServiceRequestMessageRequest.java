package com.hse.adminservice.operations.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateServiceRequestMessageRequest {
    @NotBlank
    private String text;
}
