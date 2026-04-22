package com.hse.adminservice.coworking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateServiceRequestMessageRequest {
    @NotBlank
    private String text;
}
