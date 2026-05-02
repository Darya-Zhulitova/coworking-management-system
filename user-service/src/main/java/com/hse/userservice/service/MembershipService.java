package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingInfo;
import com.hse.userservice.context.service.CurrentUserService;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.dto.request.CreateMembershipDto;
import com.hse.userservice.dto.response.CoworkingDetailsDto;
import com.hse.userservice.dto.response.MembershipDto;
import com.hse.userservice.dto.response.UserMembershipSummaryDto;
import com.hse.userservice.exception.ResourceNotFoundException;
import com.hse.userservice.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final AdminServiceClient adminServiceClient;
    private final CurrentUserService currentUserService;
    private final BalanceService balanceService;

    public MembershipDto create(CreateMembershipDto dto) {
        Long userId = currentUserService.getCurrentUserId();
        Membership membership = getOrCreateMembership(userId, dto.coworkingId());
        return toDto(membership);
    }

    public List<UserMembershipSummaryDto> getCurrentUserMemberships() {
        Long userId = currentUserService.getCurrentUserId();
        return membershipRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toSummary).toList();
    }

    public MembershipDto getById(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        Membership membership = membershipRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + id));
        return toDto(membership);
    }

    public CoworkingDetailsDto getCoworkingDetails(Long coworkingId) {
        Long userId = currentUserService.getCurrentUserId();
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(coworkingId);
        Membership membership = getOrCreateMembership(userId, coworkingId);
        return new CoworkingDetailsDto(
                coworking.id(),
                coworking.name(),
                coworking.description(),
                coworking.address(),
                coworking.workingHoursLabel(),
                coworking.heroTitle(),
                coworking.heroText(),
                coworking.imageUrls(),
                coworking.autoApproveMembership(),
                coworking.active(),
                membership.getId(),
                membership.getStatus().name().toLowerCase(),
                balanceService.getBalanceMinorUnits(membership.getId())
        );
    }

    public Membership requireOwnedMembership(Long membershipId) {
        Long userId = currentUserService.getCurrentUserId();
        return membershipRepository.findByIdAndUserId(membershipId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId));
    }

    public Membership requireOwnedMembershipByCoworkingId(Long coworkingId) {
        Long userId = currentUserService.getCurrentUserId();
        return getOrCreateMembership(userId, coworkingId);
    }

    private Membership getOrCreateMembership(Long userId, Long coworkingId) {
        return membershipRepository.findByUserIdAndCoworkingId(userId, coworkingId).orElseGet(() -> createMembership(
                userId,
                coworkingId
        ));
    }

    private Membership createMembership(Long userId, Long coworkingId) {
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(coworkingId);
        if (!coworking.active()) {
            throw new IllegalArgumentException("Coworking is inactive: " + coworkingId);
        }

        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(coworking.autoApproveMembership() ? MembershipStatus.ACTIVE : MembershipStatus.PENDING);
        membership.setCreatedAt(LocalDateTime.now());
        if (membership.getStatus() == MembershipStatus.ACTIVE) {
            membership.setApprovedAt(LocalDateTime.now());
        }
        return membershipRepository.save(membership);
    }

    private MembershipDto toDto(Membership membership) {
        return new MembershipDto(
                membership.getId(),
                membership.getUserId(),
                membership.getCoworkingId(),
                balanceService.toMajorUnits(balanceService.getBalanceMinorUnits(membership.getId())),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getApprovedAt(),
                membership.getBlockedAt()
        );
    }

    private UserMembershipSummaryDto toSummary(Membership membership) {
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(membership.getCoworkingId());
        return new UserMembershipSummaryDto(
                membership.getId(),
                membership.getCoworkingId(),
                coworking.name(),
                membership.getStatus().name().toLowerCase(),
                coworking.workingHoursLabel(),
                coworking.address(),
                balanceService.toMajorUnits(balanceService.getBalanceMinorUnits(membership.getId())),
                membership.getCreatedAt()
        );
    }
}
