package com.hse.adminservice.calendar.schedule;

import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class ScheduleBitmaskMapper {
    public static final int MONDAY_BIT = 1;
    public static final int TUESDAY_BIT = 1 << 1;
    public static final int WEDNESDAY_BIT = 1 << 2;
    public static final int THURSDAY_BIT = 1 << 3;
    public static final int FRIDAY_BIT = 1 << 4;
    public static final int SATURDAY_BIT = 1 << 5;
    public static final int SUNDAY_BIT = 1 << 6;

    public CoworkingScheduleResponse toResponse(Integer schedule) {
        int normalized = schedule == null ? 0 : schedule;
        return CoworkingScheduleResponse.builder()
                .schedule(normalized)
                .monday(hasBit(normalized, MONDAY_BIT))
                .tuesday(hasBit(normalized, TUESDAY_BIT))
                .wednesday(hasBit(normalized, WEDNESDAY_BIT))
                .thursday(hasBit(normalized, THURSDAY_BIT))
                .friday(hasBit(normalized, FRIDAY_BIT))
                .saturday(hasBit(normalized, SATURDAY_BIT))
                .sunday(hasBit(normalized, SUNDAY_BIT))
                .build();
    }

    public int toBitmask(CoworkingScheduleDaysRequest request) {
        int mask = 0;
        if (Boolean.TRUE.equals(request.getMonday())) {
            mask |= MONDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getTuesday())) {
            mask |= TUESDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getWednesday())) {
            mask |= WEDNESDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getThursday())) {
            mask |= THURSDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getFriday())) {
            mask |= FRIDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getSaturday())) {
            mask |= SATURDAY_BIT;
        }
        if (Boolean.TRUE.equals(request.getSunday())) {
            mask |= SUNDAY_BIT;
        }
        return mask;
    }

    public int bitFor(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MONDAY_BIT;
            case TUESDAY -> TUESDAY_BIT;
            case WEDNESDAY -> WEDNESDAY_BIT;
            case THURSDAY -> THURSDAY_BIT;
            case FRIDAY -> FRIDAY_BIT;
            case SATURDAY -> SATURDAY_BIT;
            case SUNDAY -> SUNDAY_BIT;
        };
    }

    public boolean hasBit(int schedule, int bit) {
        return (schedule & bit) != 0;
    }
}
