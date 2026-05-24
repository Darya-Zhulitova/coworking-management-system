package com.hse.userservice.feature.balance.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;
import com.hse.userservice.feature.balance.dto.BalanceDetailsDto;
import com.hse.userservice.feature.balance.dto.CreatePayRequestRequest;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.repository.PayRequestRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceUnitTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock MembershipService membershipService;
    @Mock UnitsService unitsService;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock PayRequestRepository payRequestRepository;
    @Mock LedgerService ledgerService;

    private BalanceService service;

    @BeforeEach
    void setUp() {
        service = new BalanceService(
                membershipService,
                unitsService,
                ledgerEntryRepository,
                payRequestRepository,
                ledgerService,
                FIXED_CLOCK
        );
    }

    @Test
    void getBalanceDetailsMapsLedgerAndPayRequestsForOwnedMembership() {
        Membership membership = membership(11L, MembershipStatus.ACTIVE);
        LedgerEntry ledger = ledger(11L, 1_000L);
        PayRequest payRequest = payRequest(71L, 11L, 2_000L, PayRequestStatus.PENDING, "receipt", null);
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership);
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(1_000L);
        when(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(11L)).thenReturn(List.of(ledger));
        when(payRequestRepository.findAllByMembershipIdOrderByCreatedAtDesc(11L)).thenReturn(List.of(payRequest));

        BalanceDetailsDto result = service.getBalanceDetails(11L);

        assertThat(result.membershipId()).isEqualTo(11L);
        assertThat(result.membershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(result.balanceMinorUnits()).isEqualTo(1_000L);
        assertThat(result.ledger()).hasSize(1);
        assertThat(result.payRequests()).hasSize(1);
        assertThat(result.payRequests().getFirst().userComment()).isEqualTo("receipt");
    }

    @Test
    void createRejectsZeroAmountBeforeLoadingMembership() {
        assertThatThrownBy(() -> service.create(11L, new CreatePayRequestRequest(0L, "zero")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Сумма");

        verifyNoInteractions(membershipService, payRequestRepository);
    }

    @Test
    void createRejectsPendingMembership() {
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership(11L, MembershipStatus.PENDING));

        assertThatThrownBy(() -> service.create(11L, new CreatePayRequestRequest(1_000L, "receipt")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("активных");

        verify(payRequestRepository, never()).save(any());
    }

    @Test
    void createPersistsPendingPayRequestWithTrimmedCommentAndFixedCreatedAt() {
        when(membershipService.requireOwnedMembershipById(11L)).thenReturn(membership(11L, MembershipStatus.ACTIVE));
        when(payRequestRepository.save(any(PayRequest.class))).thenAnswer(invocation -> {
            PayRequest saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        var result = service.create(11L, new CreatePayRequestRequest(1_000L, "  receipt photo  "));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(payRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getMembershipId()).isEqualTo(11L);
        assertThat(captor.getValue().getAmount()).isEqualTo(1_000L);
        assertThat(captor.getValue().getUserComment()).isEqualTo("receipt photo");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.status()).isEqualTo(PayRequestStatus.PENDING);
    }

    @Test
    void approvePayRequestByAdminCreatesLedgerEntryAndStoresTrimmedComment() {
        PayRequest payRequest = payRequest(71L, 11L, 2_000L, PayRequestStatus.PENDING, "receipt", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(71L, 7L)).thenReturn(Optional.of(payRequest));
        when(membershipService.requireMembershipInCoworkingForUpdate(7L, 11L)).thenReturn(membership(11L, MembershipStatus.ACTIVE));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(500L);
        when(payRequestRepository.save(payRequest)).thenReturn(payRequest);

        PayRequest result = service.approvePayRequestByAdmin(7L, 71L, "  checked  ");

        assertThat(result.getStatus()).isEqualTo(PayRequestStatus.APPROVED);
        assertThat(result.getAdminComment()).isEqualTo("checked");
        verify(ledgerService).createPayRequestEntry(11L, 2_000L, 71L, "receipt");
        verify(payRequestRepository).save(payRequest);
    }

    @Test
    void approvePayRequestByAdminRejectsNonPendingRequestAndNegativeResultingBalance() {
        PayRequest approved = payRequest(71L, 11L, 2_000L, PayRequestStatus.APPROVED, "receipt", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(71L, 7L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approvePayRequestByAdmin(7L, 71L, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ожидания");

        PayRequest withdrawal = payRequest(72L, 11L, -2_000L, PayRequestStatus.PENDING, "withdraw", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(72L, 7L)).thenReturn(Optional.of(withdrawal));
        when(membershipService.requireMembershipInCoworkingForUpdate(7L, 11L)).thenReturn(membership(11L, MembershipStatus.ACTIVE));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(1_000L);

        assertThatThrownBy(() -> service.approvePayRequestByAdmin(7L, 72L, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("отрицательным");

        verify(ledgerService, never()).createPayRequestEntry(any(), anyLong(), any(), any());
    }


    @Test
    void getPayRequestQueueMapsUsersAndFallbacks() {
        Membership knownMembership = membership(11L, MembershipStatus.ACTIVE);
        knownMembership.setUserId(41L);
        PayRequest knownRequest = payRequest(71L, 11L, 2_000L, PayRequestStatus.PENDING, "receipt", null);
        PayRequest orphanRequest = payRequest(72L, 99L, -500L, PayRequestStatus.REJECTED, "withdraw", "no");
        when(membershipService.getMembershipsForCoworking(7L)).thenReturn(List.of(knownMembership));
        when(membershipService.getUsersByMemberships(List.of(knownMembership))).thenReturn(java.util.Map.of(41L, user(41L, "Daria")));
        when(payRequestRepository.findAllByCoworkingIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(knownRequest, orphanRequest));

        var result = service.getPayRequestQueue(7L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().userId()).isEqualTo(41L);
        assertThat(result.getFirst().userName()).isEqualTo("Daria");
        assertThat(result.getFirst().status()).isEqualTo("PENDING");
        assertThat(result.get(1).userId()).isNull();
        assertThat(result.get(1).userName()).isEqualTo("Пользователь #99");
    }

    @Test
    void countPendingPayRequestsByCoworkingSaturatesToInt() {
        when(payRequestRepository.countByCoworkingIdAndStatus(7L, PayRequestStatus.PENDING)).thenReturn(4L);

        assertThat(service.countPendingPayRequestsByCoworking(7L)).isEqualTo(4);
    }

    @Test
    void rejectPayRequestByAdminRequiresPendingStatusAndComment() {
        PayRequest request = payRequest(71L, 11L, 2_000L, PayRequestStatus.PENDING, "receipt", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(71L, 7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.rejectPayRequestByAdmin(7L, 71L, " "))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("комментарий");

        PayRequest approved = payRequest(72L, 11L, 2_000L, PayRequestStatus.APPROVED, "receipt", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(72L, 7L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.rejectPayRequestByAdmin(7L, 72L, "bad receipt"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ожидания");
    }

    @Test
    void rejectPayRequestByAdminStoresRejectedStatusAndTrimmedComment() {
        PayRequest request = payRequest(71L, 11L, 2_000L, PayRequestStatus.PENDING, "receipt", null);
        when(payRequestRepository.findByIdAndCoworkingIdForUpdate(71L, 7L)).thenReturn(Optional.of(request));
        when(payRequestRepository.save(request)).thenReturn(request);

        PayRequest result = service.rejectPayRequestByAdmin(7L, 71L, "  invalid receipt  ");

        assertThat(result.getStatus()).isEqualTo(PayRequestStatus.REJECTED);
        assertThat(result.getAdminComment()).isEqualTo("invalid receipt");
        verify(payRequestRepository).save(request);
        verifyNoInteractions(ledgerService);
    }

    @Test
    void adjustBalanceByAdminRejectsZeroAndNegativeResultingBalance() {
        assertThatThrownBy(() -> service.adjustBalanceByAdmin(7L, 11L, 0L, "zero"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("не должна быть равна нулю");

        when(membershipService.requireMembershipInCoworkingForUpdate(7L, 11L)).thenReturn(membership(11L, MembershipStatus.ACTIVE));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(100L);

        assertThatThrownBy(() -> service.adjustBalanceByAdmin(7L, 11L, -200L, "penalty"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("отрицательным");

        verify(ledgerService, never()).createManualAdjustment(any(), anyLong(), any());
    }

    @Test
    void adjustBalanceByAdminDelegatesToLedgerServiceWhenResultingBalanceIsValid() {
        LedgerEntry adjustment = ledger(11L, -50L);
        when(membershipService.requireMembershipInCoworkingForUpdate(7L, 11L)).thenReturn(membership(11L, MembershipStatus.ACTIVE));
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(500L);
        when(ledgerService.createManualAdjustment(11L, -50L, "penalty")).thenReturn(adjustment);

        LedgerEntry result = service.adjustBalanceByAdmin(7L, 11L, -50L, "penalty");

        assertThat(result).isSameAs(adjustment);
    }

    private static Membership membership(Long id, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setCoworkingId(7L);
        membership.setUserId(3L);
        membership.setStatus(status);
        return membership;
    }

    private static PayRequest payRequest(
            Long id,
            Long membershipId,
            Long amount,
            PayRequestStatus status,
            String userComment,
            String adminComment
    ) {
        PayRequest request = new PayRequest();
        request.setId(id);
        request.setMembershipId(membershipId);
        request.setAmount(amount);
        request.setStatus(status);
        request.setUserComment(userComment);
        request.setAdminComment(adminComment);
        request.setCreatedAt(NOW);
        return request;
    }

    private static LedgerEntry ledger(Long membershipId, Long amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setId(1L);
        entry.setMembershipId(membershipId);
        entry.setAmount(amount);
        entry.setType(com.hse.userservice.feature.balance.domain.LedgerEntryType.BALANCE_TOP_UP);
        entry.setReferenceId(71L);
        entry.setTimestamp(NOW);
        entry.setComment("comment");
        return entry;
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
