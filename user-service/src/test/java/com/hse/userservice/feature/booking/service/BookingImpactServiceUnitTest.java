package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.feature.balance.service.LedgerService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.internal.dto.deactivation.DayClosingCommitRequest;
import com.hse.userservice.internal.dto.deactivation.DayClosingPreviewRequest;
import com.hse.userservice.internal.dto.deactivation.PlaceDeactivationCommitRequest;
import com.hse.userservice.internal.dto.deactivation.PlaceDeactivationPreviewRequest;
import com.hse.userservice.internal.dto.membershipblock.MembershipBlockCommitRequest;
import com.hse.userservice.internal.service.DeactivationImpactHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingImpactServiceUnitTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock BookingRepository bookingRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock BookingCompensationCalculator compensationCalculator;
    @Mock UserRepository userRepository;
    @Mock LedgerService ledgerService;
    @Mock DeactivationImpactHashService impactHashService;

    private BookingImpactService service;

    @BeforeEach
    void setUp() {
        service = new BookingImpactService(
                bookingRepository,
                membershipRepository,
                compensationCalculator,
                userRepository,
                ledgerService,
                impactHashService,
                FIXED_CLOCK
        );
    }

    @Test
    void previewDayClosingSortsAffectedBookingsByDateAndIdAndCalculatesTotalCompensation() {
        Booking later = booking(2L, 11L, 100L, TODAY.plusDays(2), 2_000L);
        Booking earlierSecond = booking(3L, 12L, 101L, TODAY.plusDays(1), 3_000L);
        Booking earlierFirst = booking(1L, 11L, 102L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findAllActiveByCoworkingIdAndDateIn(7L, List.of(TODAY.plusDays(1)))).thenReturn(List.of(
                later,
                earlierSecond,
                earlierFirst
        ));
        when(membershipRepository.findAllById(List.of(11L, 12L))).thenReturn(List.of(
                membership(11L, 21L),
                membership(12L, 22L)
        ));
        when(userRepository.findAllById(List.of(21L, 22L))).thenReturn(List.of(user(21L, "Daria"), user(22L, "Artem")));
        when(compensationCalculator.calculate(later)).thenReturn(2_500L);
        when(compensationCalculator.calculate(earlierSecond)).thenReturn(3_750L);
        when(compensationCalculator.calculate(earlierFirst)).thenReturn(1_250L);
        when(impactHashService.createHash(eq("DAY_CLOSING"), eq(7L), isNull(), eq(List.of(TODAY.plusDays(1))), anyList()))
                .thenReturn("sha256:preview");

        var result = service.previewDayClosing(7L, new DayClosingPreviewRequest(TODAY.plusDays(1)));

        assertThat(result.affectedBookingsCount()).isEqualTo(3);
        assertThat(result.affectedBookings()).extracting(item -> item.bookingId()).containsExactly(1L, 3L, 2L);
        assertThat(result.affectedBookings().getFirst().userName()).isEqualTo("Daria");
        assertThat(result.affectedBookings().getFirst().compensationAmount()).isEqualByComparingTo("1250");
        assertThat(result.totalCompensationAmount()).isEqualTo(7_500L);
        assertThat(result.affectedDates()).containsExactly(TODAY.plusDays(1).toString());
        assertThat(result.impactHash()).isEqualTo("sha256:preview");
    }

    @Test
    void previewUsesFallbackUserNameWhenMembershipOrUserIsMissing() {
        Booking booking = booking(1L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findAllActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqual(
                7L,
                100L,
                TODAY.plusDays(1)
        )).thenReturn(List.of(booking));
        when(membershipRepository.findAllById(List.of(11L))).thenReturn(List.of());
        when(userRepository.findAllById(List.of())).thenReturn(List.of());
        when(compensationCalculator.calculate(booking)).thenReturn(1_250L);
        when(impactHashService.createHash(eq("PLACE_DEACTIVATION"), eq(7L), eq(100L), eq(List.<java.time.LocalDate>of()), anyList()))
                .thenReturn("sha256:preview");

        var result = service.previewPlaceDeactivation(7L, new PlaceDeactivationPreviewRequest(100L));

        assertThat(result.affectedBookings()).hasSize(1);
        assertThat(result.affectedBookings().getFirst().userId()).isNull();
        assertThat(result.affectedBookings().getFirst().userName()).isEqualTo("Пользователь");
    }

    @Test
    void commitDayClosingRequiresFreshImpactHashBeforeChangingBookings() {
        Booking booking = booking(1L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(7L, List.of(TODAY.plusDays(1)))).thenReturn(List.of(booking));
        stubResponseGraph(booking, "DAY_CLOSING", null, List.of(TODAY.plusDays(1)), "sha256:actual", 1_250L);

        assertThatThrownBy(() -> service.commitDayClosing(7L, new DayClosingCommitRequest(TODAY.plusDays(1), null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("предпросмотр");
        assertThatThrownBy(() -> service.commitDayClosing(7L, new DayClosingCommitRequest(TODAY.plusDays(1), "sha256:stale")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Данные изменились");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTUAL);
        assertThat(booking.getActive()).isTrue();
        verify(bookingRepository, never()).saveAll(any());
        verify(ledgerService, never()).createAdminBookingCompensation(any(), anyLong(), any(), any());
    }

    @Test
    void commitDayClosingCancelsBookingsAndCreatesCompensationLedgerEntries() {
        Booking booking = booking(1L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(7L, List.of(TODAY.plusDays(1)))).thenReturn(List.of(booking));
        stubResponseGraph(booking, "DAY_CLOSING", null, List.of(TODAY.plusDays(1)), "sha256:actual", 1_250L);

        var result = service.commitDayClosing(7L, new DayClosingCommitRequest(TODAY.plusDays(1), "sha256:actual"));

        assertThat(result.totalCompensationAmount()).isEqualTo(1_250L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_ADMIN);
        assertThat(booking.getActive()).isFalse();
        verify(bookingRepository).saveAll(List.of(booking));
        verify(bookingRepository).flush();
        verify(ledgerService).createAdminBookingCompensation(
                11L,
                1_250L,
                1L,
                "DAY_CLOSING, бронирование BR-2026-000001"
        );
    }

    @Test
    void commitDoesNotCreateCompensationLedgerEntryWhenCompensationIsZero() {
        Booking booking = booking(1L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqualForUpdate(
                7L,
                100L,
                TODAY.plusDays(1)
        )).thenReturn(List.of(booking));
        stubResponseGraph(booking, "PLACE_DEACTIVATION", 100L, List.of(), "sha256:actual", 0L);

        service.commitPlaceDeactivation(7L, new PlaceDeactivationCommitRequest(100L, "sha256:actual"));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_ADMIN);
        verify(ledgerService, never()).createAdminBookingCompensation(any(), anyLong(), any(), any());
    }

    @Test
    void commitRejectsBookingThatIsAlreadyCanceledDuringApplyPhase() {
        Booking booking = booking(1L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        booking.setActive(false);
        when(bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(7L, List.of(TODAY.plusDays(1)))).thenReturn(List.of(booking));
        stubResponseGraph(booking, "DAY_CLOSING", null, List.of(TODAY.plusDays(1)), "sha256:actual", 1_250L);

        assertThatThrownBy(() -> service.commitDayClosing(7L, new DayClosingCommitRequest(TODAY.plusDays(1), "sha256:actual")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("уже отменено");

        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    void membershipBlockPreviewAndCommitUseMembershipBlockCompensationRules() {
        Booking booking = booking(1L, 11L, 100L, TODAY, 1_000L);
        when(bookingRepository.findAllActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqual(7L, 11L, TODAY))
                .thenReturn(List.of(booking));
        when(bookingRepository.findActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqualForUpdate(7L, 11L, TODAY))
                .thenReturn(List.of(booking));
        when(membershipRepository.findAllById(List.of(11L))).thenReturn(List.of(membership(11L, 21L)));
        when(userRepository.findAllById(List.of(21L))).thenReturn(List.of(user(21L, "Daria")));
        when(compensationCalculator.calculateMembershipBlockCompensation(booking)).thenReturn(500L);
        when(impactHashService.createHash(eq("MEMBERSHIP_BLOCK"), eq(7L), eq(11L), eq(List.<java.time.LocalDate>of()), anyList()))
                .thenReturn("sha256:block");

        var preview = service.previewMembershipBlock(7L, 11L);
        var commit = service.commitMembershipBlock(7L, 11L, new MembershipBlockCommitRequest("sha256:block").impactHash());

        assertThat(preview.totalCompensationAmount()).isEqualTo(500L);
        assertThat(commit.totalCompensationAmount()).isEqualTo(500L);
        verify(ledgerService).createAdminBookingCompensation(
                11L,
                500L,
                1L,
                "MEMBERSHIP_BLOCK, бронирование BR-2026-000001"
        );
    }


    @Test
    void previewAndCommitPlaceClosingUsePlaceDateAndRequestedDateInHash() {
        Booking booking = booking(4L, 11L, 100L, TODAY.plusDays(3), 4_000L);
        when(bookingRepository.findAllActiveByCoworkingIdAndPlaceIdAndDate(7L, 100L, TODAY.plusDays(3)))
                .thenReturn(List.of(booking));
        when(bookingRepository.findActiveByCoworkingIdAndPlaceIdAndDateForUpdate(7L, 100L, TODAY.plusDays(3)))
                .thenReturn(List.of(booking));
        when(membershipRepository.findAllById(List.of(11L))).thenReturn(List.of(membership(11L, 21L)));
        when(userRepository.findAllById(List.of(21L))).thenReturn(List.of(user(21L, "Daria")));
        when(compensationCalculator.calculate(booking)).thenReturn(1_000L);
        when(impactHashService.createHash(
                eq("PLACE_CLOSING"),
                eq(7L),
                eq(100L),
                eq(List.of(TODAY.plusDays(3))),
                anyList()
        )).thenReturn("sha256:place-closing");

        var preview = service.previewPlaceClosing(7L, new com.hse.userservice.internal.dto.deactivation.PlaceClosingPreviewRequest(
                100L,
                TODAY.plusDays(3)
        ));
        var commit = service.commitPlaceClosing(7L, new com.hse.userservice.internal.dto.deactivation.PlaceClosingCommitRequest(
                100L,
                TODAY.plusDays(3),
                "sha256:place-closing"
        ));

        assertThat(preview.affectedDates()).containsExactly(TODAY.plusDays(3).toString());
        assertThat(commit.affectedBookingsCount()).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_ADMIN);
        verify(ledgerService).createAdminBookingCompensation(
                11L,
                1_000L,
                4L,
                "PLACE_CLOSING, бронирование BR-2026-000004"
        );
    }

    @Test
    void previewAndCommitScheduleReductionUseAffectedDatesFromRequest() {
        LocalDate firstDate = TODAY.plusDays(4);
        LocalDate secondDate = TODAY.plusDays(5);
        Booking first = booking(5L, 11L, 100L, firstDate, 1_000L);
        Booking second = booking(6L, 11L, 101L, secondDate, 2_000L);
        List<LocalDate> affectedDates = List.of(secondDate, firstDate);
        when(bookingRepository.findAllActiveByCoworkingIdAndDateIn(7L, affectedDates)).thenReturn(List.of(second, first));
        when(bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(7L, affectedDates)).thenReturn(List.of(second, first));
        when(membershipRepository.findAllById(List.of(11L))).thenReturn(List.of(membership(11L, 21L)));
        when(userRepository.findAllById(List.of(21L))).thenReturn(List.of(user(21L, "Daria")));
        when(compensationCalculator.calculate(first)).thenReturn(100L);
        when(compensationCalculator.calculate(second)).thenReturn(200L);
        when(impactHashService.createHash(eq("SCHEDULE_REDUCTION"), eq(7L), isNull(), eq(affectedDates), anyList()))
                .thenReturn("sha256:schedule");

        var preview = service.previewScheduleReduction(7L, new com.hse.userservice.internal.dto.deactivation.ScheduleReductionPreviewRequest(affectedDates));
        var commit = service.commitScheduleReduction(7L, new com.hse.userservice.internal.dto.deactivation.ScheduleReductionCommitRequest(
                affectedDates,
                "sha256:schedule"
        ));

        assertThat(preview.affectedDates()).containsExactly(firstDate.toString(), secondDate.toString());
        assertThat(preview.totalCompensationAmount()).isEqualTo(300L);
        assertThat(commit.affectedBookings()).extracting(item -> item.bookingId()).containsExactly(5L, 6L);
        verify(ledgerService).createAdminBookingCompensation(11L, 100L, 5L, "SCHEDULE_REDUCTION, бронирование BR-2026-000005");
        verify(ledgerService).createAdminBookingCompensation(11L, 200L, 6L, "SCHEDULE_REDUCTION, бронирование BR-2026-000006");
    }

    @Test
    void commitRejectsBlankImpactHashBeforeApplyingCancellations() {
        Booking booking = booking(7L, 11L, 100L, TODAY.plusDays(1), 1_000L);
        when(bookingRepository.findActiveByCoworkingIdAndDateInForUpdate(7L, List.of(TODAY.plusDays(1)))).thenReturn(List.of(booking));
        stubResponseGraph(booking, "DAY_CLOSING", null, List.of(TODAY.plusDays(1)), "sha256:actual", 1_250L);

        assertThatThrownBy(() -> service.commitDayClosing(7L, new DayClosingCommitRequest(TODAY.plusDays(1), "   ")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("предпросмотр");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTUAL);
        assertThat(booking.getActive()).isTrue();
        verify(bookingRepository, never()).saveAll(any());
        verify(ledgerService, never()).createAdminBookingCompensation(any(), anyLong(), any(), any());
    }

    @Test
    void previewPlaceDeactivationUsesBookingDatesWhenRequestedDatesAreEmpty() {
        Booking first = booking(8L, 11L, 100L, TODAY.plusDays(10), 1_000L);
        Booking second = booking(9L, 11L, 100L, TODAY.plusDays(8), 1_000L);
        when(bookingRepository.findAllActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqual(7L, 100L, TODAY.plusDays(1)))
                .thenReturn(List.of(first, second));
        when(membershipRepository.findAllById(List.of(11L))).thenReturn(List.of(membership(11L, 21L)));
        when(userRepository.findAllById(List.of(21L))).thenReturn(List.of(user(21L, "Daria")));
        when(compensationCalculator.calculate(first)).thenReturn(0L);
        when(compensationCalculator.calculate(second)).thenReturn(0L);
        when(impactHashService.createHash(eq("PLACE_DEACTIVATION"), eq(7L), eq(100L), eq(List.<LocalDate>of()), anyList()))
                .thenReturn("sha256:dates");

        var result = service.previewPlaceDeactivation(7L, new PlaceDeactivationPreviewRequest(100L));

        assertThat(result.affectedDates()).containsExactly(TODAY.plusDays(8).toString(), TODAY.plusDays(10).toString());
    }

    private void stubResponseGraph(
            Booking booking,
            String operation,
            Long placeId,
            List<LocalDate> requestedDates,
            String hash,
            long compensation
    ) {
        when(membershipRepository.findAllById(List.of(booking.getMembershipId()))).thenReturn(List.of(membership(
                booking.getMembershipId(),
                21L
        )));
        when(userRepository.findAllById(List.of(21L))).thenReturn(List.of(user(21L, "Daria")));
        when(compensationCalculator.calculate(booking)).thenReturn(compensation);
        when(impactHashService.createHash(eq(operation), eq(7L), eq(placeId), eq(requestedDates), anyList())).thenReturn(hash);
    }

    private static Booking booking(Long id, Long membershipId, Long placeId, LocalDate date, long cost) {
        Booking booking = BookingServiceUnitFixtures.booking(id, membershipId, placeId, date, cost);
        return booking;
    }

    private static Membership membership(Long id, Long userId) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setUserId(userId);
        membership.setCoworkingId(7L);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setCreatedAt(NOW.minusDays(5));
        return membership;
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
