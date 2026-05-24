package com.hse.userservice.feature.membership.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.CoworkingInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceUnitTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock MembershipRepository membershipRepository;
    @Mock AdminServiceClient adminServiceClient;
    @Mock CurrentUserService currentUserService;
    @Mock UnitsService unitsService;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(membershipRepository, adminServiceClient, currentUserService, unitsService, bookingRepository, userRepository, FIXED_CLOCK);
    }

    @Test
    void getJoinPreviewReturnsCoworkingInformationFromAdminDomain() {
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, true, true));

        var preview = service.getJoinPreview("VOLGA");

        assertThat(preview.coworkingId()).isEqualTo(7L);
        assertThat(preview.name()).isEqualTo("Volga Hub");
        assertThat(preview.autoApproveMembership()).isTrue();
        assertThat(preview.active()).isTrue();
    }

    @Test
    void joinByTokenRejectsInactiveCoworkingBeforePersistingMembership() {
        when(currentUserService.getCurrentUser()).thenReturn(user(3L));
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, true, false));

        assertThatThrownBy(() -> service.joinByToken("VOLGA"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("недоступен");

        verify(membershipRepository, never()).saveAndFlush(any());
    }

    @Test
    void joinByTokenReturnsExistingMembershipWithoutCreatingDuplicate() {
        Membership existing = membership(11L, 3L, 7L, MembershipStatus.BLOCKED);
        when(currentUserService.getCurrentUser()).thenReturn(user(3L));
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, true, true));
        when(membershipRepository.findByUserIdAndCoworkingId(3L, 7L)).thenReturn(Optional.of(existing));

        var result = service.joinByToken("VOLGA");

        assertThat(result.membershipId()).isEqualTo(11L);
        assertThat(result.status()).isEqualTo("blocked");
        assertThat(result.existingMembership()).isTrue();
        verify(membershipRepository, never()).saveAndFlush(any());
    }

    @Test
    void joinByTokenCreatesActiveMembershipWhenCoworkingAutoApproves() {
        when(currentUserService.getCurrentUser()).thenReturn(user(3L));
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, true, true));
        when(membershipRepository.findByUserIdAndCoworkingId(3L, 7L)).thenReturn(Optional.empty());
        when(membershipRepository.saveAndFlush(any(Membership.class))).thenAnswer(invocation -> {
            Membership membership = invocation.getArgument(0);
            membership.setId(11L);
            return membership;
        });

        var result = service.joinByToken("VOLGA");

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(captor.getValue().getApprovedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(result.status()).isEqualTo("active");
        assertThat(result.existingMembership()).isFalse();
    }

    @Test
    void joinByTokenCreatesPendingMembershipWhenCoworkingRequiresApproval() {
        when(currentUserService.getCurrentUser()).thenReturn(user(3L));
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, false, true));
        when(membershipRepository.findByUserIdAndCoworkingId(3L, 7L)).thenReturn(Optional.empty());
        when(membershipRepository.saveAndFlush(any(Membership.class))).thenAnswer(invocation -> {
            Membership membership = invocation.getArgument(0);
            membership.setId(12L);
            return membership;
        });

        var result = service.joinByToken("VOLGA");

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MembershipStatus.PENDING);
        assertThat(captor.getValue().getApprovedAt()).isNull();
        assertThat(result.status()).isEqualTo("pending");
    }

    @Test
    void joinByTokenFallsBackToExistingMembershipWhenUniqueConstraintIsHit() {
        Membership existing = membership(11L, 3L, 7L, MembershipStatus.PENDING);
        when(currentUserService.getCurrentUser()).thenReturn(user(3L));
        when(adminServiceClient.getCoworkingInfoByJoinToken("VOLGA")).thenReturn(coworking(7L, false, true));
        when(membershipRepository.findByUserIdAndCoworkingId(3L, 7L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(membershipRepository.saveAndFlush(any(Membership.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        var result = service.joinByToken("VOLGA");

        assertThat(result.membershipId()).isEqualTo(11L);
        assertThat(result.existingMembership()).isTrue();
    }

    @Test
    void requireOwnedMembershipByIdReturnsOnlyCurrentUsersMembership() {
        Membership owned = membership(11L, 3L, 7L, MembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(membershipRepository.findById(11L)).thenReturn(Optional.of(owned));

        assertThat(service.requireOwnedMembershipById(11L)).isSameAs(owned);

        when(membershipRepository.findById(12L)).thenReturn(Optional.of(membership(12L, 99L, 7L, MembershipStatus.ACTIVE)));
        assertThatThrownBy(() -> service.requireOwnedMembershipById(12L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    @Test
    void requireMembershipInCoworkingValidatesCoworkingBoundary() {
        Membership membership = membership(11L, 3L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findById(11L)).thenReturn(Optional.of(membership));

        assertThat(service.requireMembershipInCoworking(7L, 11L)).isSameAs(membership);

        assertThatThrownBy(() -> service.requireMembershipInCoworking(8L, 11L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    @Test
    void approveByAdminAllowsOnlyPendingMembershipAndClearsBlockedAt() {
        Membership pending = membership(11L, 3L, 7L, MembershipStatus.PENDING);
        pending.setBlockedAt(NOW.minusDays(1));
        when(membershipRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(pending));
        when(membershipRepository.save(pending)).thenReturn(pending);

        Membership result = service.approveByAdmin(7L, 11L);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(result.getApprovedAt()).isEqualTo(NOW);
        assertThat(result.getBlockedAt()).isNull();

        Membership active = membership(12L, 3L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.approveByAdmin(7L, 12L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ожидания");
    }

    @Test
    void rejectByAdminAllowsOnlyPendingMembership() {
        Membership pending = membership(11L, 3L, 7L, MembershipStatus.PENDING);
        when(membershipRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(pending));
        when(membershipRepository.save(pending)).thenReturn(pending);

        Membership result = service.rejectByAdmin(7L, 11L);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.BLOCKED);
        assertThat(result.getBlockedAt()).isEqualTo(NOW);

        Membership active = membership(12L, 3L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.rejectByAdmin(7L, 12L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ожидания");
    }

    @Test
    void blockByAdminReturnsAlreadyBlockedMembershipWithoutSavingAndBlocksActiveMembership() {
        Membership blocked = membership(11L, 3L, 7L, MembershipStatus.BLOCKED);
        when(membershipRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(blocked));

        Membership first = service.blockByAdmin(7L, 11L);

        assertThat(first).isSameAs(blocked);
        verify(membershipRepository, never()).save(blocked);

        Membership active = membership(12L, 3L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(active));
        when(membershipRepository.save(active)).thenReturn(active);

        Membership second = service.blockByAdmin(7L, 12L);

        assertThat(second.getStatus()).isEqualTo(MembershipStatus.BLOCKED);
        assertThat(second.getBlockedAt()).isEqualTo(NOW);
    }


    @Test
    void getInternalMembershipListMapsUsersBalancesAndBookingCounters() {
        Membership first = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        Membership second = membership(2L, 102L, 7L, MembershipStatus.PENDING);
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(first, second));
        when(userRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(user(101L), user(102L)));
        when(unitsService.getBalancesMinorUnits(List.of(1L, 2L))).thenReturn(java.util.Map.of(1L, 1_500L, 2L, 0L));
        when(bookingRepository.findAllByMembershipIdOrderByDateDesc(1L)).thenReturn(List.of(booking(11L, 1L), booking(12L, 1L)));
        when(bookingRepository.findAllByMembershipIdOrderByDateDesc(2L)).thenReturn(List.of());

        var result = service.getInternalMembershipList(7L, null);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().membershipId()).isEqualTo(1L);
        assertThat(result.getFirst().userEmail()).isEqualTo("daria@example.test");
        assertThat(result.getFirst().balanceMinorUnits()).isEqualTo(1_500L);
        assertThat(result.getFirst().activeBookingsCount()).isEqualTo(2);
    }

    @Test
    void getInternalMembershipProfileUsesMembershipRulesAndBuildsBookingHistory() {
        Membership membership = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));
        when(userRepository.findById(101L)).thenReturn(Optional.of(user(101L)));
        when(unitsService.getBalanceMinorUnits(1L)).thenReturn(2_000L);
        when(bookingRepository.findAllByMembershipIdOrderByDateDesc(1L)).thenReturn(List.of(booking(11L, 1L)));

        var result = service.getInternalMembershipProfile(7L, 1L);

        assertThat(result.userName()).isEqualTo("Daria");
        assertThat(result.userEmail()).isEqualTo("daria@example.test");
        assertThat(result.balanceMinorUnits()).isEqualTo(2_000L);
        assertThat(result.activeBookings()).hasSize(1);
        assertThat(result.activeBookings().getFirst().bookingNumber()).isEqualTo("BR-11");
    }

    @Test
    void getAdminStatsAndTotalBalanceAreCalculatedByMembershipService() {
        List<Membership> memberships = List.of(
                membership(1L, 101L, 7L, MembershipStatus.ACTIVE),
                membership(2L, 102L, 7L, MembershipStatus.PENDING),
                membership(3L, 103L, 7L, MembershipStatus.BLOCKED)
        );
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(memberships);
        when(unitsService.getBalancesMinorUnits(List.of(1L, 2L, 3L))).thenReturn(java.util.Map.of(1L, 100L, 2L, 250L));

        var stats = service.getAdminStats(7L);
        long balance = service.getTotalBalanceMinorUnits(7L);

        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.active()).isEqualTo(1);
        assertThat(stats.pending()).isEqualTo(1);
        assertThat(stats.blocked()).isEqualTo(1);
        assertThat(balance).isEqualTo(350L);
    }


    @Test
    void getCurrentUserMembershipsMapsCoworkingSnapshotAndBalance() {
        Membership membership = membership(9L, 3L, 7L, MembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(membershipRepository.findAllByUserIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(membership));
        when(adminServiceClient.getCoworkingInfo(7L)).thenReturn(coworking(7L, true, true));
        when(unitsService.getBalanceMinorUnits(9L)).thenReturn(12_345L);
        when(unitsService.toMajorUnits(12_345L)).thenReturn(new java.math.BigDecimal("123.45"));

        var result = service.getCurrentUserMemberships();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(9L);
        assertThat(result.getFirst().coworkingName()).isEqualTo("Volga Hub");
        assertThat(result.getFirst().status()).isEqualTo("active");
        assertThat(result.getFirst().balance()).isEqualByComparingTo("123.45");
    }

    @Test
    void getMembershipCoworkingDetailsUsesOwnedMembershipAndAdminSnapshot() {
        Membership membership = membership(9L, 3L, 7L, MembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(membershipRepository.findById(9L)).thenReturn(Optional.of(membership));
        when(adminServiceClient.getCoworkingInfo(7L)).thenReturn(coworking(7L, true, true));
        when(unitsService.getBalanceMinorUnits(9L)).thenReturn(2_500L);

        var result = service.getMembershipCoworkingDetails(9L);

        assertThat(result.name()).isEqualTo("Volga Hub");
        assertThat(result.membershipId()).isEqualTo(9L);
        assertThat(result.membershipStatus()).isEqualTo("active");
        assertThat(result.balanceMinorUnits()).isEqualTo(2_500L);
    }

    @Test
    void getMembershipContextCombinesCurrentUserMembershipCoworkingAndBalance() {
        User user = user(3L);
        user.setDescription(null);
        Membership membership = membership(9L, 3L, 7L, MembershipStatus.PENDING);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(membershipRepository.findById(9L)).thenReturn(Optional.of(membership));
        when(adminServiceClient.getCoworkingInfo(7L)).thenReturn(coworking(7L, false, true));
        when(unitsService.getBalanceMinorUnits(9L)).thenReturn(0L);

        var result = service.getMembershipContext(9L);

        assertThat(result.user().id()).isEqualTo(3L);
        assertThat(result.user().description()).isEmpty();
        assertThat(result.coworking().membershipStatus()).isEqualTo("pending");
        assertThat(result.membership().id()).isEqualTo(9L);
    }

    @Test
    void getUsersByMembershipsReturnsEmptyMapWithoutRepositoryCallForEmptyMemberships() {
        assertThat(service.getUsersByMemberships(List.of())).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getCoworkingUsersUsesFallbackNameAndBalancesForReadModel() {
        Membership known = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        Membership missingUser = membership(2L, 102L, 7L, MembershipStatus.PENDING);
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(known, missingUser));
        when(userRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(user(101L)));
        when(unitsService.getBalancesMinorUnits(List.of(1L, 2L))).thenReturn(java.util.Map.of(1L, 900L));

        var result = service.getCoworkingUsers(7L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().name()).isEqualTo("Daria");
        assertThat(result.getFirst().balance()).isEqualTo(900L);
        assertThat(result.get(1).name()).isEqualTo("Пользователь #102");
        assertThat(result.get(1).balance()).isZero();
    }

    @Test
    void getInternalMembershipListSupportsSearchByIdStatusNameAndEmail() {
        Membership first = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        Membership second = membership(2L, 202L, 7L, MembershipStatus.PENDING);
        User firstUser = user(101L);
        firstUser.setName("Daria Alpha");
        firstUser.setEmail("alpha@example.test");
        User secondUser = user(202L);
        secondUser.setName("Artem Beta");
        secondUser.setEmail("beta@example.test");
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(first, second));
        when(userRepository.findAllById(List.of(101L, 202L))).thenReturn(List.of(firstUser, secondUser));
        when(unitsService.getBalancesMinorUnits(List.of(1L, 2L))).thenReturn(java.util.Map.of());
        when(bookingRepository.findAllByMembershipIdOrderByDateDesc(any())).thenReturn(List.of());

        assertThat(service.getInternalMembershipList(7L, "1")).extracting(item -> item.membershipId()).contains(1L);
        assertThat(service.getInternalMembershipList(7L, "202")).extracting(item -> item.userId()).containsExactly(202L);
        assertThat(service.getInternalMembershipList(7L, "pending")).extracting(item -> item.membershipId()).containsExactly(2L);
        assertThat(service.getInternalMembershipList(7L, "alpha")).extracting(item -> item.membershipId()).containsExactly(1L);
        assertThat(service.getInternalMembershipList(7L, "BETA@EXAMPLE.TEST")).extracting(item -> item.membershipId()).containsExactly(2L);
    }

    @Test
    void getInternalMembershipListUsesFallbackWhenUserIsMissing() {
        Membership membership = membership(1L, 404L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(membership));
        when(userRepository.findAllById(List.of(404L))).thenReturn(List.of());
        when(unitsService.getBalancesMinorUnits(List.of(1L))).thenReturn(java.util.Map.of());
        when(bookingRepository.findAllByMembershipIdOrderByDateDesc(1L)).thenReturn(List.of());

        var result = service.getInternalMembershipList(7L, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userName()).isEqualTo("Пользователь #404");
        assertThat(result.getFirst().userEmail()).isNull();
    }

    @Test
    void getInternalMembershipProfileFailsWhenUserRecordIsMissing() {
        Membership membership = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));
        when(userRepository.findById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInternalMembershipProfile(7L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    void getMembershipQueueMapsStatusLowerCaseAndFallbackUserName() {
        Membership active = membership(1L, 101L, 7L, MembershipStatus.ACTIVE);
        Membership pending = membership(2L, 102L, 7L, MembershipStatus.PENDING);
        when(membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(active, pending));
        when(userRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(user(101L)));

        var result = service.getMembershipQueue(7L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().status()).isEqualTo("active");
        assertThat(result.get(1).userName()).isEqualTo("Пользователь #102");
        assertThat(result.get(1).status()).isEqualTo("pending");
    }

    private static CoworkingInfo coworking(Long id, boolean autoApprove, boolean active) {
        return new CoworkingInfo(
                id,
                "Volga Hub",
                "Coworking description",
                "Nizhny Novgorod",
                "09:00-21:00",
                "Work here",
                "Hero text",
                List.of("https://example.test/coworking.png"),
                autoApprove,
                true,
                active
        );
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("daria@example.test");
        user.setName("Daria");
        user.setPasswordHash("hash");
        return user;
    }

    private static Membership membership(Long id, Long userId, Long coworkingId, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(status);
        membership.setCreatedAt(NOW.minusDays(1));
        return membership;
    }

    private static Booking booking(Long id, Long membershipId) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setMembershipId(membershipId);
        booking.setBookingNumber("BR-" + id);
        booking.setPlaceId(100L + id);
        booking.setDate(NOW.toLocalDate().plusDays(id));
        booking.setCost(1_000L);
        booking.setStatus(BookingStatus.ACTUAL);
        booking.setActive(true);
        return booking;
    }
}
