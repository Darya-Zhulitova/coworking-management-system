package com.hse.userservice.internal.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.booking.BookingStatus;
import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.ledger.LedgerEntryType;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.user.User;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.internal.dto.deactivation.*;
import com.hse.userservice.repository.BookingRepository;
import com.hse.userservice.repository.LedgerEntryRepository;
import com.hse.userservice.repository.MembershipRepository;
import com.hse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeactivationInternalService {
    private final BookingRepository bookingRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AdminServiceClient adminServiceClient;

    public OperationalImpactResponse preview(UserDeactivateOperationRequest request) {
        return buildResponse(request, findAffectedBookings(request), "preview");
    }

    @Transactional
    public OperationalImpactResponse commit(UserDeactivateOperationRequest request) {
        List<Booking> bookings = findAffectedBookings(request);
        OperationalImpactResponse response = buildResponse(request, bookings, "commit");
        bookings.forEach(booking -> applyAdminCancellation(request, booking));
        return response;
    }

    private List<Booking> findAffectedBookings(UserDeactivateOperationRequest request) {
        return switch (request.operationType()) {
            case "PLACE_DEACTIVATION" ->
                    bookingRepository.findAllByCoworkingIdAndPlaceIdAndActiveTrueAndDateGreaterThanEqual(
                            request.coworkingId(),
                            requirePlaceId(request),
                            LocalDate.now().plusDays(1)
                    );
            case "PLACE_CLOSING" -> bookingRepository.findAllByCoworkingIdAndPlaceIdAndActiveTrueAndDate(
                    request.coworkingId(),
                    requirePlaceId(request),
                    requireSingleDate(request)
            );
            case "CLOSE_DAY", "SCHEDULE_REDUCTION" -> bookingRepository.findAllByCoworkingIdAndActiveTrueAndDateIn(
                    request.coworkingId(),
                    requireDates(request)
            );
            default ->
                    throw new ResourceConflictException("Unsupported deactivation operation: " + request.operationType());
        };
    }

    private OperationalImpactResponse buildResponse(
            UserDeactivateOperationRequest request,
            List<Booking> bookings,
            String mode
    ) {
        Map<Long, Membership> memberships = membershipsById(bookings);
        Map<Long, User> users = usersByMembership(memberships.values());
        Map<Long, CoworkingConfigSnapshot.Place> places = placesById(request.coworkingId());
        List<AffectedBookingResponse> affected = bookings.stream()
                .sorted(Comparator.comparing(Booking::getDate)
                        .thenComparing(Booking::getId))
                .map(booking -> toAffectedBooking(booking, memberships, users, places))
                .toList();
        int total = affected.stream()
                .map(AffectedBookingResponse::compensationAmount)
                .mapToInt(BigDecimal::intValue)
                .sum();
        return new OperationalImpactResponse(
                request.operationType(),
                request.targetType(),
                request.targetId(),
                request.targetName(),
                affected.size(),
                plannedCommands(mode, affected.isEmpty()),
                affectedDates(request, bookings),
                affected,
                total,
                mode,
                affected.isEmpty() ? "Активных будущих бронирований не найдено." : "Найдено активных бронирований: " + affected.size() + ". "
        );
    }

    private AffectedBookingResponse toAffectedBooking(
            Booking booking,
            Map<Long, Membership> memberships,
            Map<Long, User> users,
            Map<Long, CoworkingConfigSnapshot.Place> places
    ) {
        Membership membership = memberships.get(booking.getMembershipId());
        User user = membership == null ? null : users.get(membership.getUserId());
        CoworkingConfigSnapshot.Place place = places.get(booking.getPlaceId());
        return new AffectedBookingResponse(
                booking.getId(),
                booking.getStatus().name(),
                booking.getDate().atStartOfDay(),
                booking.getDate().plusDays(1).atStartOfDay(),
                BigDecimal.valueOf(booking.getCost()),
                BigDecimal.valueOf(calculateCompensation(booking)),
                new BookingUserResponse(
                        user == null ? null : user.getId(),
                        user == null ? "Пользователь" : user.getName(),
                        user == null ? "" : user.getEmail()
                ),
                new BookingPlaceResponse(
                        booking.getPlaceId(),
                        place == null ? "Место #" + booking.getPlaceId() : place.name()
                )
        );
    }

    private void applyAdminCancellation(UserDeactivateOperationRequest request, Booking booking) {
        long compensation = calculateCompensation(booking);
        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELED_ADMIN);
        bookingRepository.save(booking);
        if (compensation <= 0)
            return;
        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(booking.getMembershipId());
        entry.setCoworkingId(booking.getCoworkingId());
        entry.setType(LedgerEntryType.DAY_CLOSURE_COMPENSATION);
        entry.setAmount(compensation);
        entry.setName("Компенсация за административную отмену бронирования");
        entry.setComment(request.operationType() + ": " + request.targetName() + ", бронь #" + booking.getId());
        entry.setTimestamp(LocalDateTime.now());
        ledgerEntryRepository.save(entry);
    }

    private long calculateCompensation(Booking booking) {
        return BigDecimal.valueOf(booking.getCost())
                .multiply(booking.getDayClosureCompensationCoefficient())
                .setScale(0, RoundingMode.DOWN)
                .longValue();
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

    private Map<Long, CoworkingConfigSnapshot.Place> placesById(Long coworkingId) {
        try {
            return adminServiceClient.getCoworkingConfigSnapshot(coworkingId)
                    .places()
                    .stream()
                    .collect(Collectors.toMap(CoworkingConfigSnapshot.Place::id, Function.identity()));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private List<String> affectedDates(UserDeactivateOperationRequest request, List<Booking> bookings) {
        if (request.affectedDates() != null && !request.affectedDates().isEmpty())
            return request.affectedDates().stream().map(LocalDate::toString).toList();
        return bookings.stream().map(Booking::getDate).distinct().sorted().map(LocalDate::toString).toList();
    }

    private List<String> plannedCommands(String mode, boolean empty) {
        if (empty)
            return List.of();
        return "commit".equals(mode) ? List.of("cancel-active-bookings", "write-compensation-ledger-entries") : List.of(
                "preview-active-bookings",
                "calculate-compensation"
        );
    }

    private Long requirePlaceId(UserDeactivateOperationRequest request) {
        if (request.placeId() == null)
            throw new ResourceConflictException("placeId is required for " + request.operationType());
        return request.placeId();
    }

    private LocalDate requireSingleDate(UserDeactivateOperationRequest request) {
        return requireDates(request).get(0);
    }

    private List<LocalDate> requireDates(UserDeactivateOperationRequest request) {
        List<LocalDate> dates = request.affectedDates() == null ? List.of() : request.affectedDates();
        if (dates.isEmpty())
            throw new ResourceConflictException("affectedDates is required for " + request.operationType());
        return dates;
    }
}
