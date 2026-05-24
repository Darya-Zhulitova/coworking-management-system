package com.hse.adminservice.support;

import com.hse.adminservice.calendar.exception.domain.CoworkingScheduleException;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.coworking.domain.Coworking;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ScheduleTestFactory {
    private ScheduleTestFactory() {
    }

    public static CoworkingScheduleException closedDay(Coworking coworking, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return CoworkingScheduleException.builder()
                .coworking(coworking)
                .date(date)
                .type(ScheduleExceptionType.CLOSE)
                .name("Maintenance")
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
