package com.hse.adminservice.calendar.compensation;

import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OperationalImpactResponseFactory {
    public OperationalImpactResponse noImpact(
            String operationType,
            String targetType,
            Long targetId,
            String targetName,
            LocalDate date,
            String mode
    ) {
        return OperationalImpactResponse.builder()
                .affectedBookingsCount(0)
                .affectedDates(date == null ? List.of() : List.of(date.toString()))
                .affectedBookings(List.of())
                .totalCompensationAmount(0L)
                .impactHash("NO_IMPACT")
                .build();
    }
}
