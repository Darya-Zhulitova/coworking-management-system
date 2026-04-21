package com.hse.userservice.service;

import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.domain.payrequest.PayRequest;
import com.hse.userservice.dto.request.CreatePayRequestDto;
import com.hse.userservice.dto.response.BalanceDetailsDto;
import com.hse.userservice.dto.response.LedgerEntryDto;
import com.hse.userservice.dto.response.PayRequestDto;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.repository.LedgerEntryRepository;
import com.hse.userservice.repository.PayRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayRequestService {
    private final MembershipService membershipService;
    private final BalanceService balanceService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayRequestRepository payRequestRepository;

    @Transactional(readOnly = true)
    public BalanceDetailsDto getBalanceDetails(Long coworkingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        return new BalanceDetailsDto(
                membership.getId(),
                coworkingId,
                membership.getStatus(),
                balanceService.getBalanceMinorUnits(membership.getId()),
                ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membership.getId())
                        .stream()
                        .map(this::toLedgerDto)
                        .toList(),
                payRequestRepository.findAllByMembershipIdOrderByCreatedAtDesc(membership.getId())
                        .stream()
                        .map(item -> toPayRequestDto(item, coworkingId))
                        .toList()
        );
    }

    @Transactional
    public PayRequestDto create(CreatePayRequestDto dto) {
        if (dto.amount() == 0L) {
            throw new IllegalArgumentException("amount must not be zero");
        }

        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        if (membership.getStatus() == MembershipStatus.PENDING) {
            throw new ResourceConflictException("Only active and blocked membership can create pay requests.");
        }

        PayRequest payRequest = new PayRequest();
        payRequest.setMembershipId(membership.getId());
        payRequest.setAmount(dto.amount());
        payRequest.setUserComment(dto.userComment().trim());
        PayRequest saved = payRequestRepository.save(payRequest);
        return toPayRequestDto(saved, dto.coworkingId());
    }

    private LedgerEntryDto toLedgerDto(LedgerEntry entry) {
        return new LedgerEntryDto(
                entry.getId(),
                entry.getCoworkingId(),
                entry.getTimestamp(),
                entry.getType(),
                entry.getName(),
                entry.getComment(),
                entry.getAmount()
        );
    }

    private PayRequestDto toPayRequestDto(PayRequest payRequest, Long coworkingId) {
        return new PayRequestDto(
                payRequest.getId(),
                coworkingId,
                payRequest.getAmount(),
                payRequest.getStatus(),
                payRequest.getUserComment(),
                payRequest.getAdminComment(),
                payRequest.getCreatedAt()
        );
    }
}
