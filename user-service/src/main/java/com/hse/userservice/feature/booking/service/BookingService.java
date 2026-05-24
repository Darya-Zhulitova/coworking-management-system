package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.dto.*;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.internal.dto.booking.PlaceBookingAdminDto;
import com.hse.userservice.internal.dto.booking.PlaceBookingListDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final MembershipService membershipService;
    private final AdminServiceClient adminServiceClient;
    private final UnitsService unitsService;
    private final BookingRepository bookingRepository;
    private final BookingSnapshotContextFactory contextFactory;
    private final BookingAvailabilityService availabilityService;
    private final CartPricingService cartPricingService;
    private final BookingResponseMapper responseMapper;
    private final BookingEntityFactory bookingEntityFactory;
    private final BookingLedgerService bookingLedgerService;
    private final BookingCancellationPolicy cancellationPolicy;
    private final BookingRequestIdGenerator requestIdGenerator;
    private final BookingNumberGenerator bookingNumberGenerator;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;


    @Transactional(readOnly = true)
    public PlaceBookingListDto getCurrentPlaceBookings(Long coworkingId, Long placeId) {
        List<Membership> memberships = membershipService.getMembershipsForCoworking(coworkingId);
        List<Long> membershipIds = memberships.stream().map(Membership::getId).toList();
        if (membershipIds.isEmpty()) {
            return new PlaceBookingListDto(coworkingId, placeId, "user-service", null, List.of());
        }
        Map<Long, Membership> membershipById = memberships.stream().collect(Collectors.toMap(
                Membership::getId,
                Function.identity()
        ));
        Map<Long, User> users = membershipService.getUsersByMemberships(memberships);
        List<Booking> bookings = bookingRepository.findAllByMembershipIdInAndPlaceIdAndActiveTrueAndDateGreaterThanEqualOrderByDateAscIdAsc(membershipIds,
                placeId,
                LocalDate.now(clock)
        );
        List<PlaceBookingAdminDto> response = bookings.stream().map(booking -> {
            Membership membership = membershipById.get(booking.getMembershipId());
            User user = membership == null ? null : users.get(membership.getUserId());
            return new PlaceBookingAdminDto(
                    booking.getId(),
                    booking.getBookingNumber(),
                    booking.getMembershipId(),
                    userName(user, booking.getMembershipId()),
                    booking.getDate(),
                    booking.getCost(),
                    booking.getActive(),
                    booking.getStatus().name()
            );
        }).toList();
        return new PlaceBookingListDto(coworkingId, placeId, "user-service", null, response);
    }

    @Transactional(readOnly = true)
    public BookingInitResponseDto getBookingInit(Long membershipId, LocalDate previewDate) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        BookingContext snapshot = adminServiceClient.getBookingContext(coworkingId);
        BookingSnapshotContext context = contextFactory.from(snapshot);
        LocalDate effectivePreviewDate = previewDate == null ? LocalDate.now(clock) : previewDate;

        Map<String, Boolean> reservedPairs = availabilityService.findReservedPairsForDate(
                snapshot.places().stream().map(BookingContext.Place::id).collect(Collectors.toSet()),
                effectivePreviewDate
        );
        List<BookingInitResponseDto.FloorItemDto> floors = snapshot.floors()
                .stream()
                .filter(floor -> Boolean.TRUE.equals(floor.active()))
                .map(floor -> new BookingInitResponseDto.FloorItemDto(
                        floor.id(),
                        floor.name(),
                        floor.index(),
                        floor.imageFileId(),
                        floor.imageUrl(),
                        floor.active()
                ))
                .toList();

        List<BookingInitResponseDto.PlaceItemDto> places = snapshot.places()
                .stream()
                .filter(place -> Boolean.TRUE.equals(place.active()))
                .map(place -> responseMapper.toInitPlace(place, effectivePreviewDate, context, reservedPairs))
                .toList();

        return new BookingInitResponseDto(
                snapshot.name(),
                membership.getId(),
                membership.getStatus().name().toLowerCase(),
                unitsService.getBalanceMinorUnits(membership.getId()),
                effectivePreviewDate,
                Boolean.TRUE.equals(snapshot.floorMapEnabled()),
                floors,
                places
        );
    }

    @Transactional(readOnly = true)
    public List<PlaceAvailabilityDayDto> getPlaceAvailability(
            Long membershipId,
            Long placeId,
            LocalDate from,
            LocalDate to
    ) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        requireActiveMembership(membership);
        BookingContext snapshot = adminServiceClient.getBookingContext(coworkingId);
        BookingSnapshotContext context = contextFactory.from(snapshot);
        BookingContext.Place place = snapshot.places().stream().filter(candidate -> Objects.equals(
                candidate.id(),
                placeId
        )).findFirst().orElseThrow(() -> new ResourceNotFoundException("Место не найдено: " + placeId));

        LocalDate start = from == null ? LocalDate.now(clock) : from;
        LocalDate end = to == null ? start.plusDays(30) : to;
        if (end.isBefore(start)) {
            throw new ResourceConflictException("Дата окончания периода не может быть раньше даты начала.");
        }
        if (start.plusDays(31).isBefore(end)) {
            throw new ResourceConflictException("Период проверки доступности не может превышать 31 день.");
        }

        List<LocalDate> dates = start.datesUntil(end.plusDays(1)).toList();
        Map<String, Boolean> reservedPairs = availabilityService.findReservedPairsForDates(List.of(placeId), dates);

        return dates.stream().map(date -> new PlaceAvailabilityDayDto(
                date,
                availabilityService.isPlaceAvailable(date, place, context, reservedPairs)
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingListItemDto> getBookings(Long membershipId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        List<Booking> bookings = bookingRepository.findAllByMembershipIdOrderByDateDesc(membership.getId());
        Map<Long, PlaceSummary> placesById = loadPlaceSummaries(coworkingId, bookings);

        return bookings.stream().map(item -> responseMapper.toBookingListItemFromSummary(item, placesById)).toList();
    }

    @Transactional
    public CancelBookingResponseDto cancelBooking(Long membershipId, Long bookingId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        requireActiveMembership(membership);

        Booking booking = bookingRepository.findByIdAndMembershipIdForUpdate(bookingId, membership.getId()).orElseThrow(
                () -> new ResourceNotFoundException("Бронирование не найдено: " + bookingId));
        if (booking.getStatus() != BookingStatus.ACTUAL) {
            throw new ResourceConflictException("Бронирование уже отменено.");
        }

        long refundAmount = cancellationPolicy.calculatePreview(booking);

        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELED_USER);
        bookingRepository.saveAndFlush(booking);

        if (refundAmount > 0) {
            try {
                bookingLedgerService.refundUserCancellation(membership.getId(), booking, refundAmount);
            } catch (DataIntegrityViolationException exception) {
                log.error("Failed to create refund ledger entry for booking {}", booking.getId(), exception);
                throw new ResourceConflictException("Возврат по этому бронированию уже создан.");
            }
        }

        Map<Long, PlaceSummary> placesById = loadPlaceSummaries(coworkingId, List.of(booking));

        return new CancelBookingResponseDto(
                booking.getId(),
                refundAmount,
                unitsService.getBalanceMinorUnits(membership.getId()),
                responseMapper.toBookingListItemFromSummary(booking, placesById)
        );
    }

    @Transactional(readOnly = true)
    public CartCalculationResponseDto calculateCart(Long membershipId, CartCalculateRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        requireActiveMembership(membership);
        BookingContext snapshot = adminServiceClient.getBookingContext(coworkingId);
        return cartPricingService.calculate(coworkingId, membership, snapshot, dto.items()).response();
    }

    public CreateFromCartResponseDto createFromCart(Long membershipId, CreateFromCartRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        Long coworkingId = membership.getCoworkingId();
        requireActiveMembership(membership);
        BookingContext snapshot = adminServiceClient.getBookingContext(coworkingId);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> createFromCartTransactional(membershipId, dto, snapshot));
    }

    private CreateFromCartResponseDto createFromCartTransactional(
            Long membershipId,
            CreateFromCartRequestDto dto,
            BookingContext snapshot
    ) {
        Membership membership = membershipService.requireOwnedMembershipByIdForUpdate(membershipId);
        Long coworkingId = membership.getCoworkingId();
        requireActiveMembership(membership);
        CalculatedCart cart = cartPricingService.calculate(coworkingId, membership, snapshot, dto.items());

        if (!cart.response().summary().canCheckout()) {
            throw new ResourceConflictException(firstCartError(cart.response().summary()));
        }

        String requestId = requestIdGenerator.generate();
        List<Booking> bookings = cart.resolvedItems()
                .stream()
                .map(item -> bookingEntityFactory.fromCartItem(
                        membership.getId(),
                        requestId,
                        bookingNumberGenerator.generate(),
                        item
                ))
                .toList();

        try {
            bookings = bookingRepository.saveAllAndFlush(bookings);
        } catch (DataIntegrityViolationException exception) {
            log.error(
                    "Failed to save bookings because one or more slots are no longer available. membershipId={}",
                    membership.getId(),
                    exception
            );
            throw new ResourceConflictException("Одно или несколько мест уже недоступны для бронирования.");
        }

        try {
            bookingLedgerService.chargeBookings(membership.getId(), bookings, cart.resolvedItems());
        } catch (DataIntegrityViolationException exception) {
            log.error("Failed to create booking charge ledger entry. membershipId={}", membership.getId(), exception);
            throw new ResourceConflictException("Списание уже создано для одного из этих бронирований.");
        }

        long balanceAfter = unitsService.getBalanceMinorUnits(membership.getId());
        Map<Long, BookingContext.Place> placesById = placesById(snapshot);

        return new CreateFromCartResponseDto(
                requestId,
                cart.response().summary().totalFinalPrice(),
                balanceAfter,
                bookings.stream().map(item -> responseMapper.toBookingListItem(item, placesById)).toList()
        );
    }

    private Map<Long, PlaceSummary> loadPlaceSummaries(Long coworkingId, List<Booking> bookings) {
        return adminServiceClient.getPlaceSummaries(
                coworkingId,
                bookings.stream().map(Booking::getPlaceId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(PlaceSummary::id, Function.identity()));
    }

    private Map<Long, BookingContext.Place> placesById(BookingContext snapshot) {
        return snapshot.places().stream().collect(Collectors.toMap(BookingContext.Place::id, item -> item));
    }

    private void requireActiveMembership(Membership membership) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ResourceConflictException("Бронирования доступны только для активных пользователей.");
        }
    }

    private String firstCartError(CartCalculationResponseDto.CartSummaryDto summary) {
        if (!summary.validationErrors().isEmpty()) {
            return summary.validationErrors().getFirst();
        }
        if (summary.unavailableCount() > 0) {
            return "Одно или несколько мест уже недоступны для бронирования.";
        }
        if (!Boolean.TRUE.equals(summary.hasEnoughBalance())) {
            return "На балансе недостаточно средств для бронирования.";
        }
        return "Не удалось создать бронирование из корзины.";
    }

    private String userName(User user, Long fallbackId) {
        return user == null ? "Пользователь #" + fallbackId : user.getName();
    }
}
