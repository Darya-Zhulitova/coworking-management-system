package com.hse.adminservice.schedule.dto;

import com.hse.adminservice.schedule.entity.ScheduleExceptionType;
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
