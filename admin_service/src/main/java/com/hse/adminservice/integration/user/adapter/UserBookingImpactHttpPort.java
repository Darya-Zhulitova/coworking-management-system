package com.hse.adminservice.integration.user.adapter;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.AffectedBookingResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserBookingImpactHttpPort implements UserBookingImpactPort {
    private final UserOperationDeactivationClient client;
    private final PlaceRepository placeRepository;

    @Override
    public OperationalImpactResponse previewForPlaceDeactivation(Place place) {
        return withPlaceNames(client.preview(
                place.getCoworking().getId(),
                "place-deactivation",
                Map.of("placeId", place.getId())
        ));
    }

    @Override
    public OperationalImpactResponse commitPlaceDeactivation(Place place, String impactHash) {
        return withPlaceNames(client.commit(
                place.getCoworking().getId(),
                "place-deactivation",
                Map.of("placeId", place.getId(), "impactHash", impactHash)
        ));
    }

    @Override
    public OperationalImpactResponse previewForPlaceClosing(Place place, LocalDate date, String name) {
        return withPlaceNames(client.preview(
                place.getCoworking().getId(),
                "place-closing",
                Map.of("placeId", place.getId(), "date", date)
        ));
    }

    @Override
    public OperationalImpactResponse commitPlaceClosing(Place place, LocalDate date, String name, String impactHash) {
        return withPlaceNames(client.commit(
                place.getCoworking().getId(),
                "place-closing",
                Map.of("placeId", place.getId(), "date", date, "impactHash", impactHash)
        ));
    }

    @Override
    public OperationalImpactResponse previewForCloseDay(Coworking coworking, LocalDate date, String name) {
        return withPlaceNames(client.preview(coworking.getId(), "day-closing", Map.of("date", date)));
    }

    @Override
    public OperationalImpactResponse commitCloseDay(
            Coworking coworking,
            LocalDate date,
            String name,
            String impactHash
    ) {
        return withPlaceNames(client.commit(
                coworking.getId(),
                "day-closing",
                Map.of("date", date, "impactHash", impactHash)
        ));
    }

    @Override
    public OperationalImpactResponse previewForScheduleReduction(Coworking coworking, List<LocalDate> affectedDates) {
        return withPlaceNames(client.preview(
                coworking.getId(),
                "schedule-reduction",
                Map.of("affectedDates", affectedDates == null ? List.of() : affectedDates)
        ));
    }

    @Override
    public OperationalImpactResponse commitScheduleReduction(
            Coworking coworking,
            List<LocalDate> affectedDates,
            String impactHash
    ) {
        return withPlaceNames(client.commit(
                coworking.getId(),
                "schedule-reduction",
                Map.of("affectedDates", affectedDates == null ? List.of() : affectedDates, "impactHash", impactHash)
        ));
    }

    private OperationalImpactResponse withPlaceNames(OperationalImpactResponse response) {
        if (response == null || response.affectedBookings() == null || response.affectedBookings().isEmpty()) {
            return response;
        }
        Map<Long, Place> placesById = placeRepository.findAllById(response.affectedBookings().stream().map(
                        AffectedBookingResponse::placeId).filter(id -> id != null && id > 0).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        return OperationalImpactResponse.builder()
                .affectedBookingsCount(response.affectedBookingsCount())
                .affectedDates(response.affectedDates())
                .affectedBookings(response.affectedBookings()
                        .stream()
                        .map(item -> enrichPlace(item, placesById))
                        .toList())
                .totalCompensationAmount(response.totalCompensationAmount())
                .impactHash(response.impactHash())
                .build();
    }

    private AffectedBookingResponse enrichPlace(AffectedBookingResponse item, Map<Long, Place> placesById) {
        Place place = placesById.get(item.placeId());
        String placeName = place == null ? item.placeName() : place.getName();
        return AffectedBookingResponse.builder()
                .bookingId(item.bookingId())
                .bookingNumber(item.bookingNumber())
                .membershipId(item.membershipId())
                .userId(item.userId())
                .userName(item.userName())
                .placeId(item.placeId())
                .placeName(placeName)
                .date(item.date())
                .bookingAmount(item.bookingAmount())
                .compensationAmount(item.compensationAmount())
                .build();
    }
}
