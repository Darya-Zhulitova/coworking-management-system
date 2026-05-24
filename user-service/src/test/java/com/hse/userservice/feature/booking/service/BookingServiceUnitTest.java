package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import com.hse.userservice.feature.booking.dto.CartCalculationResponseDto;
import com.hse.userservice.feature.booking.dto.CreateFromCartRequestDto;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {
    @Mock MembershipService membershipService;
    @Mock AdminServiceClient adminServiceClient;
    @Mock UnitsService unitsService;
    @Mock BookingRepository bookingRepository;
    @Mock BookingAvailabilityService availabilityService;
    @Mock CartPricingService cartPricingService;
    @Mock BookingLedgerService bookingLedgerService;
    @Mock BookingCancellationPolicy cancellationPolicy;
    @Mock BookingRequestIdGenerator requestIdGenerator;
    @Mock BookingNumberGenerator bookingNumberGenerator;

    private BookingResponseMapper responseMapper;
    private BookingService service;

    @BeforeEach
    void setUp() {
        responseMapper = new BookingResponseMapper(availabilityService, cancellationPolicy);
        service = new BookingService(
                membershipService,
                adminServiceClient,
                unitsService,
                bookingRepository,
                new BookingSnapshotContextFactory(),
                availabilityService,
                cartPricingService,
                responseMapper,
                new BookingEntityFactory(),
                bookingLedgerService,
                cancellationPolicy,
                requestIdGenerator,
                bookingNumberGenerator,
                BookingServiceUnitFixtures.CLOCK,
                new NoOpTransactionManager()
        );
    }


    @Test
    void getCurrentPlaceBookingsDelegatesMembershipLookupAndMapsUserNames() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 1_500L);
        when(membershipService.getMembershipsForCoworking(7L)).thenReturn(List.of(membership));
        when(membershipService.getUsersByMemberships(List.of(membership))).thenReturn(Map.of(10L, user(10L, "Daria")));
        when(bookingRepository.findAllByMembershipIdInAndPlaceIdAndActiveTrueAndDateGreaterThanEqualOrderByDateAscIdAsc(
                List.of(11L),
                100L,
                BookingServiceUnitFixtures.TODAY
        )).thenReturn(List.of(booking));

        var result = service.getCurrentPlaceBookings(7L, 100L);

        assertThat(result.coworkingId()).isEqualTo(7L);
        assertThat(result.placeId()).isEqualTo(100L);
        assertThat(result.bookings()).hasSize(1);
        assertThat(result.bookings().getFirst().userName()).isEqualTo("Daria");
        assertThat(result.bookings().getFirst().status()).isEqualTo(BookingStatus.ACTUAL.name());
    }

    @Test
    void getCurrentPlaceBookingsReturnsEmptyResponseWhenCoworkingHasNoMemberships() {
        when(membershipService.getMembershipsForCoworking(7L)).thenReturn(List.of());

        var result = service.getCurrentPlaceBookings(7L, 100L);

        assertThat(result.bookings()).isEmpty();
        verify(bookingRepository, never()).findAllByMembershipIdInAndPlaceIdAndActiveTrueAndDateGreaterThanEqualOrderByDateAscIdAsc(
                anyList(),
                anyLong(),
                any()
        );
    }

    @Test
    void getBookingInitUsesCurrentDateWhenPreviewDateIsMissingAndMapsActivePlaces() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        BookingContext snapshot = BookingServiceUnitFixtures.bookingContext();
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(snapshot);
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(5_000L);
        when(availabilityService.findReservedPairsForDate(Set.of(100L), BookingServiceUnitFixtures.TODAY)).thenReturn(Map.of());
        when(availabilityService.isPlaceAvailable(
                eq(BookingServiceUnitFixtures.TODAY),
                any(BookingContext.Place.class),
                any(BookingSnapshotContext.class),
                anyMap()
        )).thenReturn(true);

        var result = service.getBookingInit(11L, null);

        assertThat(result.membershipId()).isEqualTo(11L);
        assertThat(result.membershipStatus()).isEqualTo("active");
        assertThat(result.balanceMinorUnits()).isEqualTo(5_000L);
        assertThat(result.previewDate()).isEqualTo(BookingServiceUnitFixtures.TODAY);
        assertThat(result.floors()).hasSize(1);
        assertThat(result.places()).hasSize(1);
        assertThat(result.places().getFirst().previewImageUrl()).isEqualTo("https://example.test/place-preview.png");
        assertThat(result.places().getFirst().fullImageUrl()).isEqualTo("https://example.test/place.png");
        assertThat(result.places().getFirst().available()).isTrue();
    }

    @Test
    void getPlaceAvailabilityUsesDefaultDateRangeAndChecksEachDay() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        BookingContext snapshot = BookingServiceUnitFixtures.bookingContext();
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(snapshot);
        when(availabilityService.findReservedPairsForDates(eq(List.of(100L)), anyList())).thenReturn(Map.of());
        when(availabilityService.isPlaceAvailable(
                any(LocalDate.class),
                any(BookingContext.Place.class),
                any(BookingSnapshotContext.class),
                anyMap()
        )).thenReturn(true);

        var result = service.getPlaceAvailability(11L, 100L, null, null);

        assertThat(result).hasSize(31);
        assertThat(result.getFirst().date()).isEqualTo(BookingServiceUnitFixtures.TODAY);
        assertThat(result.getLast().date()).isEqualTo(BookingServiceUnitFixtures.TODAY.plusDays(30));
        assertThat(result).allMatch(day -> Boolean.TRUE.equals(day.available()));
    }

    @Test
    void getPlaceAvailabilityRejectsInactiveMembershipInvalidPeriodAndMissingPlace() {
        Membership pending = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        pending.setStatus(MembershipStatus.PENDING);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(pending);

        assertThatThrownBy(() -> service.getPlaceAvailability(11L, 100L, null, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("активных");

        Membership active = BookingServiceUnitFixtures.activeMembership(12L, 7L);
        when(membershipService.requireOwnedMembershipById(12L)).thenReturn(active);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(BookingServiceUnitFixtures.bookingContext());

        assertThatThrownBy(() -> service.getPlaceAvailability(
                12L,
                100L,
                BookingServiceUnitFixtures.TODAY.plusDays(3),
                BookingServiceUnitFixtures.TODAY.plusDays(2)
        )).isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("раньше даты начала");

        assertThatThrownBy(() -> service.getPlaceAvailability(
                12L,
                100L,
                BookingServiceUnitFixtures.TODAY,
                BookingServiceUnitFixtures.TODAY.plusDays(32)
        )).isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("31 день");

        assertThatThrownBy(() -> service.getPlaceAvailability(12L, 999L, BookingServiceUnitFixtures.TODAY, BookingServiceUnitFixtures.TODAY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Место не найдено");
    }

    @Test
    void cancelBookingRejectsAlreadyCanceledBooking() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        Booking canceled = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        canceled.setStatus(BookingStatus.CANCELED_USER);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(bookingRepository.findByIdAndMembershipIdForUpdate(1L, 11L)).thenReturn(java.util.Optional.of(canceled));

        assertThatThrownBy(() -> service.cancelBooking(11L, 1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("уже отменено");

        verify(bookingRepository, never()).saveAndFlush(any());
        verify(bookingLedgerService, never()).refundUserCancellation(any(), any(), anyLong());
    }

    @Test
    void cancelBookingWithZeroRefundCancelsBookingWithoutLedgerRefund() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        Booking booking = BookingServiceUnitFixtures.booking(2L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(bookingRepository.findByIdAndMembershipIdForUpdate(2L, 11L)).thenReturn(java.util.Optional.of(booking));
        when(cancellationPolicy.calculatePreview(booking)).thenReturn(0L);
        when(adminServiceClient.getPlaceSummaries(7L, Set.of(100L))).thenReturn(List.of(new PlaceSummary(
                100L,
                "Desk A",
                "Floor A",
                "Desk",
                "https://example.test/preview.png",
                "https://example.test/full.png"
        )));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(0L);

        var result = service.cancelBooking(11L, 2L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_USER);
        assertThat(booking.getActive()).isFalse();
        assertThat(result.refundMinorUnits()).isZero();
        assertThat(result.balanceAfterMinorUnits()).isZero();
        assertThat(result.booking().placePreviewImageUrl()).isEqualTo("https://example.test/preview.png");
        verify(bookingRepository).saveAndFlush(booking);
        verify(bookingLedgerService, never()).refundUserCancellation(any(), any(), anyLong());
    }

    @Test
    void cancelBookingCreatesRefundAndReturnsUpdatedBalanceAndMappedBooking() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(bookingRepository.findByIdAndMembershipIdForUpdate(1L, 11L)).thenReturn(java.util.Optional.of(booking));
        when(cancellationPolicy.calculatePreview(booking)).thenReturn(1_200L);
        when(adminServiceClient.getPlaceSummaries(7L, Set.of(100L))).thenReturn(List.of(new PlaceSummary(
                100L,
                "Desk A",
                "Floor A",
                "Desk",
                "https://example.test/preview.png",
                "https://example.test/full.png"
        )));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(3_000L);

        var result = service.cancelBooking(11L, 1L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_USER);
        assertThat(booking.getActive()).isFalse();
        assertThat(result.refundMinorUnits()).isEqualTo(1_200L);
        assertThat(result.balanceAfterMinorUnits()).isEqualTo(3_000L);
        assertThat(result.booking().placePreviewImageUrl()).isEqualTo("https://example.test/preview.png");
        verify(bookingLedgerService).refundUserCancellation(11L, booking, 1_200L);
    }

    @Test
    void cancelBookingWrapsDuplicateRefundLedgerEntryAsConflict() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(bookingRepository.findByIdAndMembershipIdForUpdate(1L, 11L)).thenReturn(java.util.Optional.of(booking));
        when(cancellationPolicy.calculatePreview(booking)).thenReturn(1_200L);
        when(bookingLedgerService.refundUserCancellation(11L, booking, 1_200L)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.cancelBooking(11L, 1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Возврат");
    }

    @Test
    void calculateCartRequiresActiveMembershipAndDelegatesToCartPricingService() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        BookingContext snapshot = BookingServiceUnitFixtures.bookingContext();
        var request = new com.hse.userservice.feature.booking.dto.CartCalculateRequestDto(List.of(new BookingCartItemRequestDto(
                100L,
                BookingServiceUnitFixtures.TODAY.plusDays(1)
        )));
        var response = cartResponse(true, true, 1_500L, List.of());
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(snapshot);
        when(cartPricingService.calculate(7L, membership, snapshot, request.items())).thenReturn(new CalculatedCart(response, List.of()));

        assertThat(service.calculateCart(11L, request)).isSameAs(response);
    }

    @Test
    void createFromCartRejectsCartSummaryErrorsBeforeSavingBookings() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        BookingContext snapshot = BookingServiceUnitFixtures.bookingContext();
        var dto = new CreateFromCartRequestDto(List.of(new BookingCartItemRequestDto(100L, BookingServiceUnitFixtures.TODAY.plusDays(1))));
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(membershipService.requireOwnedMembershipByIdForUpdate(11L)).thenReturn(membership);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(snapshot);
        when(cartPricingService.calculate(7L, membership, snapshot, dto.items())).thenReturn(new CalculatedCart(
                cartResponse(false, true, 0L, List.of("Выбранная дата недоступна")),
                List.of()
        ));

        assertThatThrownBy(() -> service.createFromCart(11L, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Выбранная дата недоступна");

        verify(bookingRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void createFromCartWrapsSlotUniqueConstraintConflict() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 7L);
        BookingContext snapshot = BookingServiceUnitFixtures.bookingContext();
        var item = new BookingCartItemRequestDto(100L, BookingServiceUnitFixtures.TODAY.plusDays(1));
        var resolvedItem = new ResolvedCartItem(
                100L,
                "Place 100",
                item.date(),
                "Floor 10",
                "Desk",
                snapshot.tariffs().getFirst(),
                1_500L,
                true
        );
        var dto = new CreateFromCartRequestDto(List.of(item));
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(membershipService.requireOwnedMembershipByIdForUpdate(11L)).thenReturn(membership);
        when(adminServiceClient.getBookingContext(7L)).thenReturn(snapshot);
        when(cartPricingService.calculate(7L, membership, snapshot, dto.items())).thenReturn(new CalculatedCart(
                cartResponse(true, true, 1_500L, List.of()),
                List.of(resolvedItem)
        ));
        when(requestIdGenerator.generate()).thenReturn("REQ-1");
        when(bookingNumberGenerator.generate()).thenReturn("BR-1");
        when(bookingRepository.saveAllAndFlush(anyList())).thenThrow(new DataIntegrityViolationException("duplicate slot"));

        assertThatThrownBy(() -> service.createFromCart(11L, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("уже недоступны");

        verify(bookingLedgerService, never()).chargeBookings(any(), anyList(), anyList());
    }

    private static CartCalculationResponseDto cartResponse(
            boolean canCheckout,
            boolean hasEnoughBalance,
            long total,
            List<String> errors
    ) {
        return new CartCalculationResponseDto(
                List.of(),
                new CartCalculationResponseDto.CartSummaryDto(total, 0, errors, hasEnoughBalance, 5_000L - total, canCheckout)
        );
    }

    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.test");
        user.setPasswordHash("hash");
        return user;
    }
}
