package com.hse.userservice.feature.membership.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.dto.*;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.dto.UserResponse;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.internal.dto.CoworkingUserReadModelDto;
import com.hse.userservice.internal.dto.MembershipQueueItemDto;
import com.hse.userservice.internal.dto.membership.InternalMembershipBookingDto;
import com.hse.userservice.internal.dto.membership.InternalMembershipListItemDto;
import com.hse.userservice.internal.dto.membership.InternalMembershipProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final AdminServiceClient adminServiceClient;
    private final CurrentUserService currentUserService;
    private final UnitsService unitsService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Clock clock;


    public List<UserMembershipSummaryDto> getCurrentUserMemberships() {
        Long userId = currentUserService.getCurrentUserId();
        return membershipRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toSummary).toList();
    }

    public CoworkingDetailsDto getMembershipCoworkingDetails(Long membershipId) {
        Membership membership = requireOwnedMembershipById(membershipId);
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(membership.getCoworkingId());
        return toCoworkingDetailsDto(coworking, membership, unitsService.getBalanceMinorUnits(membership.getId()));
    }

    public CoworkingContextDto getMembershipContext(Long membershipId) {
        User user = currentUserService.getCurrentUser();
        Membership membership = requireOwnedMembershipById(membershipId);
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(membership.getCoworkingId());
        Long balanceMinorUnits = unitsService.getBalanceMinorUnits(membership.getId());
        return new CoworkingContextDto(
                toUserProfileDto(user),
                toCoworkingDetailsDto(coworking, membership, balanceMinorUnits),
                new MembershipContextDto(
                        membership.getId(),
                        membership.getStatus().name().toLowerCase(),
                        balanceMinorUnits
                )
        );
    }


    public JoinCoworkingPreviewDto getJoinPreview(String joinToken) {
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfoByJoinToken(joinToken);
        return new JoinCoworkingPreviewDto(
                coworking.id(),
                coworking.name(),
                coworking.description(),
                coworking.address(),
                coworking.workingHoursLabel(),
                coworking.heroTitle(),
                coworking.heroText(),
                coworking.imageUrls(),
                coworking.autoApproveMembership(),
                coworking.active()
        );
    }

    @Transactional
    public JoinCoworkingResultDto joinByToken(String joinToken) {
        User user = currentUserService.getCurrentUser();
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfoByJoinToken(joinToken);
        if (!coworking.active()) {
            throw new ResourceConflictException("Коворкинг недоступен для присоединения.");
        }
        Membership membership = membershipRepository.findByUserIdAndCoworkingId(user.getId(), coworking.id()).orElse(
                null);
        if (membership != null) {
            return toJoinResult(membership, true);
        }

        Membership created = new Membership();
        created.setUserId(user.getId());
        created.setCoworkingId(coworking.id());
        created.setCreatedAt(LocalDateTime.now(clock));
        if (coworking.autoApproveMembership()) {
            created.setStatus(com.hse.userservice.feature.membership.domain.MembershipStatus.ACTIVE);
            created.setApprovedAt(LocalDateTime.now(clock));
        } else {
            created.setStatus(MembershipStatus.PENDING);
        }
        try {
            return toJoinResult(membershipRepository.saveAndFlush(created), false);
        } catch (DataIntegrityViolationException exception) {
            log.error(
                    "Failed to create membership because membership may already exist. userId={}, coworkingId={}",
                    user.getId(),
                    coworking.id(),
                    exception
            );
            Membership existing = membershipRepository.findByUserIdAndCoworkingId(user.getId(), coworking.id())
                    .orElseThrow(() -> exception);
            return toJoinResult(existing, true);
        }
    }

    public Membership requireOwnedMembershipById(Long membershipId) {
        Long userId = currentUserService.getCurrentUserId();
        return membershipRepository.findById(membershipId)
                .filter(membership -> membership.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден: " + membershipId));
    }

    public Membership requireOwnedMembershipByIdForUpdate(Long membershipId) {
        Long userId = currentUserService.getCurrentUserId();
        return membershipRepository.findByIdForUpdate(membershipId)
                .filter(membership -> membership.getUserId()
                        .equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден: " + membershipId));
    }

    @Transactional
    public Membership approveByAdmin(Long coworkingId, Long membershipId) {
        Membership membership = requireMembershipInCoworkingForUpdate(coworkingId, membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResourceConflictException("Пользователя можно подтвердить только из статуса ожидания.");
        }
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setApprovedAt(LocalDateTime.now(clock));
        membership.setBlockedAt(null);
        return membershipRepository.save(membership);
    }

    @Transactional
    public Membership rejectByAdmin(Long coworkingId, Long membershipId) {
        Membership membership = requireMembershipInCoworkingForUpdate(coworkingId, membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResourceConflictException("Заявку пользователя можно отклонить только из статуса ожидания.");
        }
        membership.setStatus(MembershipStatus.BLOCKED);
        membership.setBlockedAt(LocalDateTime.now(clock));
        return membershipRepository.save(membership);
    }

    @Transactional
    public Membership blockByAdmin(Long coworkingId, Long membershipId) {
        Membership membership = requireMembershipInCoworkingForUpdate(coworkingId, membershipId);
        if (membership.getStatus() == MembershipStatus.BLOCKED) {
            return membership;
        }
        membership.setStatus(MembershipStatus.BLOCKED);
        membership.setBlockedAt(LocalDateTime.now(clock));
        return membershipRepository.save(membership);
    }

    public Membership requireMembershipInCoworking(Long coworkingId, Long membershipId) {
        return membershipRepository.findById(membershipId).filter(membership -> membership.getCoworkingId()
                .equals(coworkingId)).orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден."));
    }

    public Membership requireMembershipInCoworkingForUpdate(Long coworkingId, Long membershipId) {
        return membershipRepository.findByIdForUpdate(membershipId).filter(membership -> membership.getCoworkingId()
                .equals(coworkingId)).orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден."));
    }


    public List<Membership> getMembershipsForCoworking(Long coworkingId) {
        return membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
    }

    public Map<Long, User> getUsersByMemberships(List<Membership> memberships) {
        List<Long> userIds = memberships.stream().map(Membership::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public List<CoworkingUserReadModelDto> getCoworkingUsers(Long coworkingId) {
        List<Membership> memberships = getMembershipsForCoworking(coworkingId);
        Map<Long, User> users = getUsersByMemberships(memberships);
        Map<Long, Long> balances = unitsService.getBalancesMinorUnits(membershipIds(memberships));
        return memberships.stream().map(membership -> new CoworkingUserReadModelDto(
                membership.getUserId(),
                userName(users.get(membership.getUserId()), membership.getUserId()),
                membership.getCreatedAt().toLocalDate(),
                balances.getOrDefault(membership.getId(), 0L),
                null,
                null
        )).toList();
    }

    public List<InternalMembershipListItemDto> getInternalMembershipList(Long coworkingId, String search) {
        List<Membership> memberships = getMembershipsForCoworking(coworkingId);
        Map<Long, User> users = getUsersByMemberships(memberships);
        Map<Long, Long> balances = unitsService.getBalancesMinorUnits(membershipIds(memberships));
        String normalizedSearch = normalizeSearch(search);
        return memberships.stream().filter(membership -> matchesMembershipSearch(
                membership,
                users.get(membership.getUserId()),
                normalizedSearch
        )).map(membership -> {
            User user = users.get(membership.getUserId());
            return new InternalMembershipListItemDto(
                    membership.getId(),
                    membership.getUserId(),
                    userName(user, membership.getUserId()),
                    user == null ? null : user.getEmail(),
                    membership.getStatus().name(),
                    membership.getCreatedAt(),
                    membership.getApprovedAt(),
                    membership.getBlockedAt(),
                    balances.getOrDefault(membership.getId(), 0L),
                    bookingsForMembership(membership.getId()).size()
            );
        }).toList();
    }

    public InternalMembershipProfileDto getInternalMembershipProfile(Long coworkingId, Long membershipId) {
        Membership membership = requireMembershipInCoworking(coworkingId, membershipId);
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "Пользователь не найден."));
        return new InternalMembershipProfileDto(
                membership.getId(),
                membership.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getDescription(),
                membership.getStatus().name(),
                membership.getCreatedAt(),
                membership.getApprovedAt(),
                membership.getBlockedAt(),
                unitsService.getBalanceMinorUnits(membership.getId()),
                bookingsForMembership(membership.getId()).stream().map(this::toMembershipBookingDto).toList()
        );
    }

    public List<MembershipQueueItemDto> getMembershipQueue(Long coworkingId) {
        List<Membership> memberships = getMembershipsForCoworking(coworkingId);
        Map<Long, User> users = getUsersByMemberships(memberships);
        return memberships.stream().map(item -> new MembershipQueueItemDto(
                item.getId(),
                item.getUserId(),
                userName(users.get(item.getUserId()), item.getUserId()),
                null,
                item.getStatus().name().toLowerCase(Locale.ROOT),
                item.getCreatedAt().toLocalDate()
        )).toList();
    }

    public AdminMembershipStats getAdminStats(Long coworkingId) {
        List<Membership> memberships = getMembershipsForCoworking(coworkingId);
        return new AdminMembershipStats(
                memberships.size(),
                toInt(memberships.stream().filter(item -> item.getStatus() == MembershipStatus.ACTIVE).count()),
                toInt(memberships.stream().filter(item -> item.getStatus() == MembershipStatus.PENDING).count()),
                toInt(memberships.stream().filter(item -> item.getStatus() == MembershipStatus.BLOCKED).count())
        );
    }

    public long getTotalBalanceMinorUnits(Long coworkingId) {
        return unitsService.getBalancesMinorUnits(membershipIds(getMembershipsForCoworking(coworkingId)))
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    private CoworkingDetailsDto toCoworkingDetailsDto(
            CoworkingInfo coworking,
            Membership membership,
            Long balanceMinorUnits
    ) {
        return new CoworkingDetailsDto(
                coworking.name(),
                coworking.description(),
                coworking.address(),
                coworking.workingHoursLabel(),
                coworking.heroTitle(),
                coworking.heroText(),
                coworking.imageUrls(),
                coworking.active(),
                membership.getId(),
                membership.getStatus().name().toLowerCase(),
                balanceMinorUnits
        );
    }

    private JoinCoworkingResultDto toJoinResult(Membership membership, boolean existingMembership) {
        return new JoinCoworkingResultDto(
                membership.getId(),
                membership.getCoworkingId(),
                membership.getStatus().name().toLowerCase(),
                existingMembership
        );
    }

    private UserResponse toUserProfileDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getDescription() == null ? "" : user.getDescription()
        );
    }

    private UserMembershipSummaryDto toSummary(Membership membership) {
        CoworkingInfo coworking = adminServiceClient.getCoworkingInfo(membership.getCoworkingId());
        return new UserMembershipSummaryDto(
                membership.getId(),
                coworking.name(),
                membership.getStatus().name().toLowerCase(),
                coworking.workingHoursLabel(),
                coworking.address(),
                unitsService.toMajorUnits(unitsService.getBalanceMinorUnits(membership.getId()))
        );
    }

    private List<Booking> bookingsForMembership(Long membershipId) {
        return bookingRepository.findAllByMembershipIdOrderByDateDesc(membershipId)
                .stream()
                .sorted(Comparator.comparing(Booking::getDate).thenComparingLong(Booking::getId))
                .toList();
    }

    private InternalMembershipBookingDto toMembershipBookingDto(Booking booking) {
        return new InternalMembershipBookingDto(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getPlaceId(),
                booking.getDate(),
                booking.getCost(),
                booking.getStatus().name()
        );
    }

    private List<Long> membershipIds(List<Membership> memberships) {
        return memberships.stream().map(Membership::getId).toList();
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : "";
    }

    private boolean matchesMembershipSearch(Membership membership, User user, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        return String.valueOf(membership.getId()).contains(search) || String.valueOf(membership.getUserId()).contains(
                search) || membership.getStatus()
                .name()
                .toLowerCase(Locale.ROOT)
                .contains(search) || (user != null && user.getName() != null && user.getName()
                .toLowerCase(Locale.ROOT)
                .contains(search)) || (user != null && user.getEmail() != null && user.getEmail()
                .toLowerCase(Locale.ROOT)
                .contains(search));
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

    public record AdminMembershipStats(
            Integer total,
            Integer active,
            Integer pending,
            Integer blocked
    ) {
    }
}
