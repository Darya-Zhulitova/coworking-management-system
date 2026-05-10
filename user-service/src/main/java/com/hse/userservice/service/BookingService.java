package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.booking.BookingStatus;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.dto.request.CartCalculateRequestDto;
import com.hse.userservice.dto.request.CreateFromCartRequestDto;
import com.hse.userservice.dto.response.*;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.exception.ResourceNotFoundException;
import com.hse.userservice.repository.BookingRepository;
import com.hse.userservice.service.booking.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final MembershipService membershipService;
    private final AdminServiceClient adminServiceClient;
    private final BalanceService balanceService;
    private final BookingRepository bookingRepository;
    private final BookingSnapshotContextFactory contextFactory;
    private final BookingAvailabilityService availabilityService;
    private final CartPricingService cartPricingService;
    private final BookingResponseMapper responseMapper;
    private final BookingEntityFactory bookingEntityFactory;
    private final BookingLedgerService bookingLedgerService;
    private final BookingCancellationPolicy cancellationPolicy;
    private final BookingRequestIdGenerator requestIdGenerator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BookingInitResponseDto getBookingInit(Long coworkingId, LocalDate previewDate) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        BookingSnapshotContext context = contextFactory.from(snapshot);
        LocalDate effectivePreviewDate = previewDate == null ? LocalDate.now(clock) : previewDate;

        Map<String, Boolean> reservedPairs = availabilityService.findReservedPairsForDate(
                snapshot.places().stream().map(CoworkingConfigSnapshot.Place::id).collect(Collectors.toSet()),
                effectivePreviewDate
        );
        List<BookingInitResponseDto.PlaceItemDto> availablePlaces = snapshot.places()
                .stream()
                .map(place -> responseMapper.toInitPlace(place, effectivePreviewDate, context, reservedPairs))
                .filter(place -> Boolean.TRUE.equals(place.previewAvailable()))
                .toList();

        return new BookingInitResponseDto(
                coworkingId,
                snapshot.name(),
                membership.getId(),
                membership.getStatus().name().toLowerCase(),
                balanceService.getBalanceMinorUnits(membership.getId()),
                effectivePreviewDate,
                availablePlaces
        );
    }

    @Transactional(readOnly = true)
    public List<BookingListItemDto> getBookings(Long coworkingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        Map<Long, CoworkingConfigSnapshot.Place> placesById = placesById(snapshot);

        return bookingRepository.findAllByMembershipIdOrderByDateDesc(membership.getId())
                .stream()
                .map(item -> responseMapper.toBookingListItem(item, placesById))
                .toList();
    }

    @Transactional
    public CancelBookingResponseDto cancelBooking(Long coworkingId, Long bookingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        requireActiveMembership(membership);

        Booking booking = bookingRepository.findByIdAndMembershipId(bookingId, membership.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!Objects.equals(booking.getCoworkingId(), coworkingId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.ACTUAL) {
            throw new ResourceConflictException("Booking is already cancelled.");
        }

        long refundAmount = cancellationPolicy.calculatePreview(booking);
        if (refundAmount <= 0) {
            throw new ResourceConflictException("This booking can no longer be cancelled.");
        }

        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELED_USER);
        bookingRepository.save(booking);
        bookingLedgerService.refundUserCancellation(membership.getId(), coworkingId, booking, refundAmount);

        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        Map<Long, CoworkingConfigSnapshot.Place> placesById = placesById(snapshot);

        return new CancelBookingResponseDto(
                booking.getId(),
                coworkingId,
                refundAmount,
                balanceService.getBalanceMinorUnits(membership.getId()),
                responseMapper.toBookingListItem(booking, placesById)
        );
    }

    @Transactional(readOnly = true)
    public CartCalculationResponseDto calculateCart(CartCalculateRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        requireActiveMembership(membership);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(dto.coworkingId());
        return cartPricingService.calculate(dto.coworkingId(), membership, snapshot, dto.items()).response();
    }

    @Transactional
    public CreateFromCartResponseDto createFromCart(CreateFromCartRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        requireActiveMembership(membership);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(dto.coworkingId());
        CalculatedCart cart = cartPricingService.calculate(dto.coworkingId(), membership, snapshot, dto.items());

        if (!cart.response().summary().canCheckout()) {
            throw new ResourceConflictException(firstCartError(cart.response().summary()));
        }

        String requestId = requestIdGenerator.generate();
        List<Booking> bookings = cart.resolvedItems()
                .stream()
                .map(item -> bookingEntityFactory.fromCartItem(dto.coworkingId(), membership.getId(), requestId, item))
                .toList();

        try {
            bookingRepository.saveAll(bookings);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("One or more booking slots are no longer available.");
        }

        bookingLedgerService.chargeBooking(
                membership.getId(),
                dto.coworkingId(),
                cart.response().summary().totalFinalPrice(),
                cart.resolvedItems()
        );

        long balanceAfter = balanceService.getBalanceMinorUnits(membership.getId());
        Map<Long, CoworkingConfigSnapshot.Place> placesById = placesById(snapshot);

        return new CreateFromCartResponseDto(
                dto.coworkingId(),
                requestId,
                cart.response().summary().totalFinalPrice(),
                balanceAfter,
                bookings.stream().map(item -> responseMapper.toBookingListItem(item, placesById)).toList()
        );
    }

    private Map<Long, CoworkingConfigSnapshot.Place> placesById(CoworkingConfigSnapshot snapshot) {
        return snapshot.places().stream().collect(Collectors.toMap(CoworkingConfigSnapshot.Place::id, item -> item));
    }

    private void requireActiveMembership(Membership membership) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ResourceConflictException("Only active membership can create bookings.");
        }
    }

    private String firstCartError(CartCalculationResponseDto.CartSummaryDto summary) {
        if (!summary.validationErrors().isEmpty()) {
            return summary.validationErrors().getFirst();
        }
        if (summary.unavailableCount() > 0) {
            return "One or more booking slots are no longer available.";
        }
        if (!Boolean.TRUE.equals(summary.hasEnoughBalance())) {
            return "Insufficient balance for checkout.";
        }
        return "Unable to create booking from cart.";
    }
}
