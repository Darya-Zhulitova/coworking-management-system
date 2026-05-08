package com.hse.adminservice.calendar.exception.dto;

import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class CoworkingScheduleExceptionResponse {
    Long id;
    LocalDate date;
    ScheduleExceptionType type;
    String name;
    Boolean active;
}
