package com.hse.adminservice.unit.schedule;

import com.hse.adminservice.calendar.schedule.ScheduleBitmaskMapper;
import com.hse.adminservice.calendar.schedule.ScheduleReductionCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleReductionCalculatorTest {
    private final ScheduleBitmaskMapper mapper = new ScheduleBitmaskMapper();
    private final ScheduleReductionCalculator calculator = new ScheduleReductionCalculator(mapper);

    @Test
    void returnsOnlyDatesThatWereOpenAndBecomeClosedInsideHorizon() {
        LocalDate monday = LocalDate.of(2026, 6, 1);
        int previousMask = ScheduleBitmaskMapper.MONDAY_BIT
                | ScheduleBitmaskMapper.TUESDAY_BIT
                | ScheduleBitmaskMapper.WEDNESDAY_BIT;
        int nextMask = ScheduleBitmaskMapper.MONDAY_BIT;

        List<LocalDate> removedDates = calculator.computeRemovedDates(
                monday,
                7,
                previousMask,
                nextMask,
                Set.of()
        );

        assertThat(removedDates).containsExactly(
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 3)
        );
    }

    @Test
    void skipsForcedOpenDatesEvenWhenRegularScheduleRemovesThatWeekday() {
        LocalDate monday = LocalDate.of(2026, 6, 1);
        int previousMask = ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.TUESDAY_BIT;
        int nextMask = ScheduleBitmaskMapper.MONDAY_BIT;
        LocalDate forcedOpenTuesday = LocalDate.of(2026, 6, 2);

        List<LocalDate> removedDates = calculator.computeRemovedDates(
                monday,
                14,
                previousMask,
                nextMask,
                Set.of(forcedOpenTuesday)
        );

        assertThat(removedDates).containsExactly(LocalDate.of(2026, 6, 9));
    }

    @Test
    void returnsEmptyListWhenScheduleExpansionDoesNotRemoveOpenDays() {
        LocalDate monday = LocalDate.of(2026, 6, 1);
        int previousMask = ScheduleBitmaskMapper.MONDAY_BIT;
        int nextMask = ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.TUESDAY_BIT;

        List<LocalDate> removedDates = calculator.computeRemovedDates(monday, 7, previousMask, nextMask, Set.of());

        assertThat(removedDates).isEmpty();
    }
}
