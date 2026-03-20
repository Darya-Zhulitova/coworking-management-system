package com.hse.adminservice.exception;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ApiError {
    int status;
    String message;
    LocalDateTime timestamp;
}
