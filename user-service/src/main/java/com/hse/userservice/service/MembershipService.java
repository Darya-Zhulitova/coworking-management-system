package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.context.service.CurrentUserService;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.dto.request.CreateMembershipDto;
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

    private Membership getOrCreateMembership(Long userId, Long coworkingId) {
        return membershipRepository.findByUserIdAndCoworkingId(userId, coworkingId).orElseGet(() -> createMembership(
                userId,
                coworkingId
        ));
    }

    private Membership createMembership(Long userId, Long coworkingId) {
           Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(MembershipStatus.ACTIVE);
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
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getApprovedAt(),
                membership.getBlockedAt()
        );
    }

    private UserMembershipSummaryDto toSummary(Membership membership) {
        return new UserMembershipSummaryDto(
                membership.getId(),
                membership.getCoworkingId(),
                membership.getCreatedAt()
        );
    }
}
