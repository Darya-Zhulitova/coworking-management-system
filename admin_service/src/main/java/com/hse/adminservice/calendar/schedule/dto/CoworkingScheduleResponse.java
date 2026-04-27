package com.hse.adminservice.calendar.schedule.dto;

import lombok.Builder;

@Builder
public record CoworkingScheduleResponse(
        Integer schedule,
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday
) {
}
