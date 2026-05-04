package com.hse.adminservice.calendar.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScheduleReductionCalculator {
    private final ScheduleBitmaskMapper scheduleBitmaskMapper;

    public List<LocalDate> computeRemovedDates(
            LocalDate startDate,
            int horizonDays,
            int previousMask,
            int nextMask,
            Set<LocalDate> forcedOpenDates
    ) {
        List<LocalDate> affectedDates = new ArrayList<>();
        LocalDate endExclusive = startDate.plusDays(horizonDays);
        for (LocalDate cursor = startDate; cursor.isBefore(endExclusive); cursor = cursor.plusDays(1)) {
            int dayBit = scheduleBitmaskMapper.bitFor(cursor.getDayOfWeek());
            boolean wasOpen = scheduleBitmaskMapper.hasBit(previousMask, dayBit);
            boolean nowOpen = scheduleBitmaskMapper.hasBit(nextMask, dayBit);
            if (wasOpen && !nowOpen && !forcedOpenDates.contains(cursor)) {
                affectedDates.add(cursor);
            }
        }
        return affectedDates;
    }
}
