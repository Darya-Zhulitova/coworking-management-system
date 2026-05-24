package com.hse.userservice.feature.balance.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;
import com.hse.userservice.feature.balance.dto.BalanceDetailsDto;
import com.hse.userservice.feature.balance.dto.CreatePayRequestRequest;
import com.hse.userservice.feature.balance.dto.LedgerEntryDto;
import com.hse.userservice.feature.balance.dto.PayRequestDto;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.repository.PayRequestRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.internal.dto.PayRequestQueueItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final MembershipService membershipService;
    private final UnitsService unitsService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayRequestRepository payRequestRepository;
    private final LedgerService ledgerService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BalanceDetailsDto getBalanceDetails(Long membershipId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        return new BalanceDetailsDto(
                membership.getId(),
                membership.getStatus(),
                unitsService.getBalanceMinorUnits(membership.getId()),
                ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membership.getId())
                        .stream()
                        .map(this::toLedgerDto)
                        .toList(),
                payRequestRepository.findAllByMembershipIdOrderByCreatedAtDesc(membership.getId())
                        .stream()
                        .map(this::toPayRequestDto)
                        .toList()
        );
    }

    @Transactional
    public PayRequestDto create(Long membershipId, CreatePayRequestRequest dto) {
        if (dto.amount() == 0L) {
            throw new IllegalArgumentException("Сумма не должна быть равна нулю");
        }

        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        if (membership.getStatus() == MembershipStatus.PENDING) {
            throw new ResourceConflictException(
                    "Изменение баланса доступно только для активных и заблокированных пользователей.");
        }

        PayRequest payRequest = new PayRequest();
        payRequest.setMembershipId(membership.getId());
        payRequest.setAmount(dto.amount());
        payRequest.setUserComment(dto.userComment().trim());
        payRequest.setCreatedAt(LocalDateTime.now(clock));
        PayRequest saved = payRequestRepository.save(payRequest);
        return toPayRequestDto(saved);
    }


    @Transactional(readOnly = true)
    public List<PayRequestQueueItemDto> getPayRequestQueue(Long coworkingId) {
        List<Membership> memberships = membershipService.getMembershipsForCoworking(coworkingId);
        Map<Long, Membership> membershipById = memberships.stream().collect(Collectors.toMap(
                Membership::getId,
                Function.identity()
        ));
        Map<Long, User> users = membershipService.getUsersByMemberships(memberships);
        return payRequestRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId).stream().map(item -> {
            Membership membership = membershipById.get(item.getMembershipId());
            User user = membership == null ? null : users.get(membership.getUserId());
            return new PayRequestQueueItemDto(
                    item.getId(),
                    item.getMembershipId(),
                    membership == null ? null : membership.getUserId(),
                    userName(user, item.getMembershipId()),
                    item.getAmount(),
                    item.getStatus().name(),
                    item.getUserComment(),
                    item.getAdminComment(),
                    item.getCreatedAt().toLocalDate()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public int countPendingPayRequestsByCoworking(Long coworkingId) {
        return toInt(payRequestRepository.countByCoworkingIdAndStatus(coworkingId, PayRequestStatus.PENDING));
    }

    @Transactional
    public PayRequest approvePayRequestByAdmin(Long coworkingId, Long payRequestId, String adminComment) {
        PayRequest payRequest = requirePayRequestForCoworkingForUpdate(coworkingId, payRequestId);
        if (payRequest.getStatus() != PayRequestStatus.PENDING) {
            throw new ResourceConflictException("Платежную заявку можно подтвердить только из статуса ожидания.");
        }
        Membership membership = membershipService.requireMembershipInCoworkingForUpdate(
                coworkingId,
                payRequest.getMembershipId()
        );
        long resultingBalance = unitsService.getBalanceMinorUnits(membership.getId()) + payRequest.getAmount();
        if (resultingBalance < 0) {
            throw new ResourceConflictException("Подтверждение платежной заявки сделает баланс отрицательным.");
        }
        ledgerService.createPayRequestEntry(
                membership.getId(),
                payRequest.getAmount(),
                payRequest.getId(),
                payRequest.getUserComment()
        );
        payRequest.setStatus(PayRequestStatus.APPROVED);
        if (StringUtils.hasText(adminComment)) {
            payRequest.setAdminComment(adminComment.trim());
        }
        return payRequestRepository.save(payRequest);
    }

    @Transactional
    public PayRequest rejectPayRequestByAdmin(Long coworkingId, Long payRequestId, String adminComment) {
        PayRequest payRequest = requirePayRequestForCoworkingForUpdate(coworkingId, payRequestId);
        if (payRequest.getStatus() != PayRequestStatus.PENDING) {
            throw new ResourceConflictException("Платежную заявку можно отклонить только из статуса ожидания.");
        }
        if (!StringUtils.hasText(adminComment)) {
            throw new ResourceConflictException("Укажите комментарий администратора при отклонении платежной заявки.");
        }
        payRequest.setStatus(PayRequestStatus.REJECTED);
        payRequest.setAdminComment(adminComment.trim());
        return payRequestRepository.save(payRequest);
    }

    @Transactional
    public LedgerEntry adjustBalanceByAdmin(
            Long coworkingId,
            Long membershipId,
            long amountMinorUnits,
            String comment
    ) {
        if (amountMinorUnits == 0) {
            throw new ResourceConflictException("Сумма корректировки не должна быть равна нулю.");
        }
        Membership membership = membershipService.requireMembershipInCoworkingForUpdate(coworkingId, membershipId);
        long balanceAfter = unitsService.getBalanceMinorUnits(membership.getId()) + amountMinorUnits;
        if (balanceAfter < 0) {
            throw new ResourceConflictException("Корректировка сделает баланс отрицательным.");
        }
        return ledgerService.createManualAdjustment(membership.getId(), amountMinorUnits, comment);
    }

    private PayRequest requirePayRequestForCoworkingForUpdate(Long coworkingId, Long payRequestId) {
        return payRequestRepository.findByIdAndCoworkingIdForUpdate(payRequestId, coworkingId)
                .orElseThrow(() -> new ResourceConflictException("Платежная заявка для коворкинга не найдена."));
    }

    private LedgerEntryDto toLedgerDto(LedgerEntry entry) {
        return new LedgerEntryDto(
                entry.getId(),
                entry.getTimestamp(),
                entry.getType(),
                entry.getType().getDisplayName(),
                entry.getComment(),
                entry.getAmount()
        );
    }

    private PayRequestDto toPayRequestDto(PayRequest payRequest) {
        return new PayRequestDto(
                payRequest.getId(),
                payRequest.getAmount(),
                payRequest.getStatus(),
                payRequest.getUserComment(),
                payRequest.getAdminComment(),
                payRequest.getCreatedAt()
        );
    }

    private String userName(User user, Long fallbackId) {
        return user == null ? "Пользователь #" + fallbackId : user.getName();
    }

    private int toInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
