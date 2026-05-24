package com.hse.adminservice.unit.schedule;

import com.hse.adminservice.calendar.schedule.ScheduleBitmaskMapper;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleBitmaskMapperTest {
    private final ScheduleBitmaskMapper mapper = new ScheduleBitmaskMapper();

    @Test
    void convertsNullableScheduleToClosedWeekResponse() {
        CoworkingScheduleResponse response = mapper.toResponse(null);

        assertThat(response.schedule()).isZero();
        assertThat(response.monday()).isFalse();
        assertThat(response.tuesday()).isFalse();
        assertThat(response.wednesday()).isFalse();
        assertThat(response.thursday()).isFalse();
        assertThat(response.friday()).isFalse();
        assertThat(response.saturday()).isFalse();
        assertThat(response.sunday()).isFalse();
    }

    @Test
    void mapsSelectedDaysToBitmaskAndBack() {
        CoworkingScheduleDaysRequest request = new CoworkingScheduleDaysRequest();
        request.setMonday(true);
        request.setTuesday(false);
        request.setWednesday(true);
        request.setThursday(false);
        request.setFriday(true);
        request.setSaturday(false);
        request.setSunday(true);

        int mask = mapper.toBitmask(request);
        CoworkingScheduleResponse response = mapper.toResponse(mask);

        assertThat(mask).isEqualTo(
                ScheduleBitmaskMapper.MONDAY_BIT
                        | ScheduleBitmaskMapper.WEDNESDAY_BIT
                        | ScheduleBitmaskMapper.FRIDAY_BIT
                        | ScheduleBitmaskMapper.SUNDAY_BIT
        );
        assertThat(response.monday()).isTrue();
        assertThat(response.tuesday()).isFalse();
        assertThat(response.wednesday()).isTrue();
        assertThat(response.thursday()).isFalse();
        assertThat(response.friday()).isTrue();
        assertThat(response.saturday()).isFalse();
        assertThat(response.sunday()).isTrue();
    }

    @Test
    void exposesStableBitForEachDayOfWeek() {
        assertThat(mapper.bitFor(DayOfWeek.MONDAY)).isEqualTo(ScheduleBitmaskMapper.MONDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.TUESDAY)).isEqualTo(ScheduleBitmaskMapper.TUESDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.WEDNESDAY)).isEqualTo(ScheduleBitmaskMapper.WEDNESDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.THURSDAY)).isEqualTo(ScheduleBitmaskMapper.THURSDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.FRIDAY)).isEqualTo(ScheduleBitmaskMapper.FRIDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.SATURDAY)).isEqualTo(ScheduleBitmaskMapper.SATURDAY_BIT);
        assertThat(mapper.bitFor(DayOfWeek.SUNDAY)).isEqualTo(ScheduleBitmaskMapper.SUNDAY_BIT);
    }
}
