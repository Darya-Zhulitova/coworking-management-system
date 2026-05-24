package com.hse.adminservice.testinfra;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.AffectedBookingResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.domain.Place;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FakeUserBookingImpactPort implements UserBookingImpactPort {
    private final List<String> calls = new ArrayList<>();

    public List<String> calls() {
        return calls;
    }

    public void clear() {
        calls.clear();
    }

    @Override
    public OperationalImpactResponse previewForPlaceDeactivation(Place place) {
        calls.add("previewForPlaceDeactivation:%d".formatted(place.getId()));
        return impact(place, null, "PREVIEW_PLACE_DEACTIVATION");
    }

    @Override
    public OperationalImpactResponse commitPlaceDeactivation(Place place, String impactHash) {
        calls.add("commitPlaceDeactivation:%d:%s".formatted(place.getId(), impactHash));
        return impact(place, null, impactHash);
    }

    @Override
    public OperationalImpactResponse previewForPlaceClosing(Place place, LocalDate date, String name) {
        calls.add("previewForPlaceClosing:%d:%s:%s".formatted(place.getId(), date, name));
        return impact(place, date, "PREVIEW_PLACE_CLOSING");
    }

    @Override
    public OperationalImpactResponse commitPlaceClosing(Place place, LocalDate date, String name, String impactHash) {
        calls.add("commitPlaceClosing:%d:%s:%s:%s".formatted(place.getId(), date, name, impactHash));
        return impact(place, date, impactHash);
    }

    @Override
    public OperationalImpactResponse previewForCloseDay(Coworking coworking, LocalDate date, String name) {
        calls.add("previewForCloseDay:%d:%s:%s".formatted(coworking.getId(), date, name));
        return impact(coworking, date, "PREVIEW_CLOSE_DAY");
    }

    @Override
    public OperationalImpactResponse commitCloseDay(Coworking coworking, LocalDate date, String name, String impactHash) {
        calls.add("commitCloseDay:%d:%s:%s:%s".formatted(coworking.getId(), date, name, impactHash));
        return impact(coworking, date, impactHash);
    }

    @Override
    public OperationalImpactResponse previewForScheduleReduction(Coworking coworking, List<LocalDate> affectedDates) {
        calls.add("previewForScheduleReduction:%d:%d".formatted(coworking.getId(), affectedDates == null ? 0 : affectedDates.size()));
        return impact(coworking, affectedDates, "PREVIEW_SCHEDULE_REDUCTION");
    }

    @Override
    public OperationalImpactResponse commitScheduleReduction(Coworking coworking, List<LocalDate> affectedDates, String impactHash) {
        calls.add("commitScheduleReduction:%d:%d:%s".formatted(coworking.getId(), affectedDates == null ? 0 : affectedDates.size(), impactHash));
        return impact(coworking, affectedDates, impactHash);
    }

    private OperationalImpactResponse impact(Place place, LocalDate date, String impactHash) {
        return OperationalImpactResponse.builder()
                .affectedBookingsCount(1)
                .affectedDates(date == null ? List.of() : List.of(date.toString()))
                .affectedBookings(List.of(AffectedBookingResponse.builder()
                        .bookingId(1001L)
                        .bookingNumber("B-1001")
                        .membershipId(2001L)
                        .userId(3001L)
                        .userName("Resident")
                        .placeId(place.getId())
                        .placeName(place.getName())
                        .date(date)
                        .bookingAmount(new BigDecimal("1500.00"))
                        .compensationAmount(new BigDecimal("1500.00"))
                        .build()))
                .totalCompensationAmount(150000L)
                .impactHash(impactHash)
                .build();
    }

    private OperationalImpactResponse impact(Coworking coworking, LocalDate date, String impactHash) {
        return impact(coworking, date == null ? List.of() : List.of(date), impactHash);
    }

    private OperationalImpactResponse impact(Coworking coworking, List<LocalDate> dates, String impactHash) {
        List<String> affectedDates = dates == null ? List.of() : dates.stream().map(LocalDate::toString).toList();
        LocalDate firstDate = dates == null || dates.isEmpty() ? null : dates.get(0);
        return OperationalImpactResponse.builder()
                .affectedBookingsCount(1)
                .affectedDates(affectedDates)
                .affectedBookings(List.of(AffectedBookingResponse.builder()
                        .bookingId(1002L)
                        .bookingNumber("B-1002")
                        .membershipId(2002L)
                        .userId(3002L)
                        .userName("Resident")
                        .placeId(0L)
                        .placeName(coworking.getName())
                        .date(firstDate)
                        .bookingAmount(new BigDecimal("1500.00"))
                        .compensationAmount(new BigDecimal("1500.00"))
                        .build()))
                .totalCompensationAmount(150000L)
                .impactHash(impactHash)
                .build();
    }
}
