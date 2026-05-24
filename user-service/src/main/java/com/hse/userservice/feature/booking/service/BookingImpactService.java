package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.feature.balance.service.LedgerService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.internal.dto.deactivation.*;
import com.hse.userservice.internal.service.DeactivationImpactHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingImpactService {
    private static final String PLACE_DEACTIVATION = "PLACE_DEACTIVATION";
    private static final String PLACE_CLOSING = "PLACE_CLOSING";
    private static final String DAY_CLOSING = "DAY_CLOSING";
    private static final String SCHEDULE_REDUCTION = "SCHEDULE_REDUCTION";
    private static final String MEMBERSHIP_BLOCK = "MEMBERSHIP_BLOCK";

    private final BookingRepository bookingRepository;
    private final MembershipRepository membershipRepository;
    private final BookingCompensationCalculator compensationCalculator;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final DeactivationImpactHashService impactHashService;
    private final Clock clock;

    public OperationalImpactResponse previewPlaceDeactivation(
            Long coworkingId,
            PlaceDeactivationPreviewRequest request
    ) {
        List<Booking> bookings = bookingRepository.findAllActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqual(coworkingId,
                request.placeId(),
                LocalDate.now(clock).plusDays(1)
        );
        return buildResponse(PLACE_DEACTIVATION, coworkingId, request.placeId(), List.of(), bookings);
    }

    @Transactional
    public OperationalImpactResponse commitPlaceDeactivation(Long coworkingId, PlaceDeactivationCommitRequest request) {
        List<Booking> bookings = bookingRepository.findActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqualForUpdate(coworkingId,
                request.placeId(),
                LocalDate.now(clock).plusDays(1)
        );
        OperationalImpactResponse response = buildResponse(
                PLACE_DEACTIVATION,
                coworkingId,
                request.placeId(),
                List.of(),
                bookings
        );
        validateImpactHash(request.impactHash(), response);
        applyAdminCancellations(PLACE_DEACTIVATION, bookings);
        return response;
    }

    public OperationalImpactResponse previewPlaceClosing(Long coworkingId, PlaceClosingPreviewRequest request) {
        List<Booking> bookings = bookingRepository.findAllActiveByCoworkingIdAndPlaceIdAndDate(
                coworkingId,
                request.placeId(),
                request.date()
        );
        return buildResponse(PLACE_CLOSING, coworkingId, request.placeId(), List.of(request.date()), bookings);
    }

    @Transactional
    public OperationalImpactResponse commitPlaceClosing(Long coworkingId, PlaceClosingCommitRequest request) {
        List<Booking> bookings = bookingRepository.findActiveByCoworkingIdAndPlaceIdAndDateForUpdate(
                coworkingId,
                request.placeId(),
                request.date()
        );
        OperationalImpactResponse response = buildResponse(
                PLACE_CLOSING,
                coworkingId,
                request.placeId(),
                List.of(request.date()),
                bookings
        );
        validateImpactHash(request.impactHash(), response);
        applyAdminCancellations(PLACE_CLOSING, bookings);
        return response;
    }

    public OperationalImpactResponse previewDayClosing(Long coworkingId, DayClosingPreviewRequest request) {
        List<Booking> bookings = bookingRepository.findAllActiveByCoworkingIdAndDateIn(
                coworkingId,
                List.of(request.date())
        );
        return buildResponse(DAY_CLOSING, coworkingId, null, List.of(request.date()), bookings);
    }

    @Transactional
    public OperationalImpactResponse commitDayClosing(Long coworkingId, DayClosingCommitRequest request) {
        List<Booking> bookings = bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(
                coworkingId,
                List.of(request.date())
        );
        OperationalImpactResponse response = buildResponse(
                DAY_CLOSING,
                coworkingId,
                null,
                List.of(request.date()),
                bookings
        );
        validateImpactHash(request.impactHash(), response);
        applyAdminCancellations(DAY_CLOSING, bookings);
        return response;
    }

    public OperationalImpactResponse previewScheduleReduction(
            Long coworkingId,
            ScheduleReductionPreviewRequest request
    ) {
        List<Booking> bookings = bookingRepository.findAllActiveByCoworkingIdAndDateIn(
                coworkingId,
                request.affectedDates()
        );
        return buildResponse(SCHEDULE_REDUCTION, coworkingId, null, request.affectedDates(), bookings);
    }

    @Transactional
    public OperationalImpactResponse commitScheduleReduction(Long coworkingId, ScheduleReductionCommitRequest request) {
        List<Booking> bookings = bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(
                coworkingId,
                request.affectedDates()
        );
        OperationalImpactResponse response = buildResponse(
                SCHEDULE_REDUCTION,
                coworkingId,
                null,
                request.affectedDates(),
                bookings
        );
        validateImpactHash(request.impactHash(), response);
        applyAdminCancellations(SCHEDULE_REDUCTION, bookings);
        return response;
    }

    public OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId) {
        List<Booking> bookings = bookingRepository.findAllActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqual(coworkingId,
                membershipId,
                LocalDate.now(clock)
        );
        return buildResponse(MEMBERSHIP_BLOCK, coworkingId, membershipId, List.of(), bookings);
    }

    @Transactional
    public OperationalImpactResponse commitMembershipBlock(Long coworkingId, Long membershipId, String impactHash) {
        List<Booking> bookings = bookingRepository.findActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqualForUpdate(coworkingId,
                membershipId,
                LocalDate.now(clock)
        );
        OperationalImpactResponse response = buildResponse(
                MEMBERSHIP_BLOCK,
                coworkingId,
                membershipId,
                List.of(),
                bookings
        );
        validateImpactHash(impactHash, response);
        applyAdminCancellations(MEMBERSHIP_BLOCK, bookings);
        return response;
    }

    private OperationalImpactResponse buildResponse(
            String operation,
            Long coworkingId,
            Long placeId,
            List<LocalDate> requestedDates,
            List<Booking> bookings
    ) {
        Map<Long, Membership> memberships = membershipsById(bookings);
        Map<Long, User> users = usersByMembership(memberships.values());
        List<AffectedBookingResponse> affected = bookings.stream().sorted(Comparator.comparing(Booking::getDate)
                .thenComparing(Booking::getId)).map(booking -> toAffectedBooking(
                booking,
                memberships,
                users,
                operation
        )).toList();
        long total = affected.stream()
                .map(AffectedBookingResponse::compensationAmount)
                .mapToLong(BigDecimal::longValue)
                .sum();
        return new OperationalImpactResponse(
                affected.size(),
                affectedDates(requestedDates, bookings),
                affected,
                total,
                impactHashService.createHash(operation, coworkingId, placeId, requestedDates, bookings)
        );
    }

    private void validateImpactHash(String expectedImpactHash, OperationalImpactResponse response) {
        if (expectedImpactHash == null || expectedImpactHash.isBlank()) {
            throw new ResourceConflictException("Сначала выполните предпросмотр изменений, затем подтвердите действие");
        }
        if (!expectedImpactHash.equals(response.impactHash())) {
            throw new ResourceConflictException(
                    "Данные изменились. Обновите предпросмотр перед подтверждением действия.");
        }
    }

    private AffectedBookingResponse toAffectedBooking(
            Booking booking,
            Map<Long, Membership> memberships,
            Map<Long, User> users,
            String operation
    ) {
        Membership membership = memberships.get(booking.getMembershipId());
        User user = membership == null ? null : users.get(membership.getUserId());
        return new AffectedBookingResponse(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getMembershipId(),
                user == null ? null : user.getId(),
                user == null ? "Пользователь" : user.getName(),
                booking.getPlaceId(),
                "Место",
                booking.getDate(),
                BigDecimal.valueOf(booking.getCost()),
                BigDecimal.valueOf(compensationForOperation(booking, operation))
        );
    }

    private void applyAdminCancellations(String operation, List<Booking> bookings) {
        for (Booking booking : bookings) {
            if (booking.getStatus() != BookingStatus.ACTUAL || !Boolean.TRUE.equals(booking.getActive())) {
                throw new ResourceConflictException("Бронирование уже отменено.");
            }
            booking.setActive(false);
            booking.setStatus(BookingStatus.CANCELED_ADMIN);
        }
        bookingRepository.saveAll(bookings);
        bookingRepository.flush();

        for (Booking booking : bookings) {
            long compensation = compensationForOperation(booking, operation);
            if (compensation > 0) {
                ledgerService.createAdminBookingCompensation(
                        booking.getMembershipId(),
                        compensation,
                        booking.getId(),
                        operation + ", бронирование " + booking.getBookingNumber()
                );
            }
        }
    }


    private long compensationForOperation(Booking booking, String operation) {
        if (MEMBERSHIP_BLOCK.equals(operation)) {
            return compensationCalculator.calculateMembershipBlockCompensation(booking);
        }
        return compensationCalculator.calculate(booking);
    }

    private Map<Long, Membership> membershipsById(List<Booking> bookings) {
        return membershipRepository.findAllById(bookings.stream().map(Booking::getMembershipId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Membership::getId, Function.identity()));
    }

    private Map<Long, User> usersByMembership(Collection<Membership> memberships) {
        return userRepository.findAllById(memberships.stream().map(Membership::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private List<String> affectedDates(List<LocalDate> requestedDates, List<Booking> bookings) {
        if (requestedDates != null && !requestedDates.isEmpty()) {
            return requestedDates.stream().sorted().map(LocalDate::toString).toList();
        }
        return bookings.stream().map(Booking::getDate).distinct().sorted().map(LocalDate::toString).toList();
    }
}
