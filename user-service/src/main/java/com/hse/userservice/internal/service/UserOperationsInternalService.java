package com.hse.userservice.internal.service;

import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.ledger.LedgerEntryType;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.domain.message.Message;
import com.hse.userservice.domain.message.MessageAuthorType;
import com.hse.userservice.domain.payrequest.PayRequest;
import com.hse.userservice.domain.payrequest.PayRequestStatus;
import com.hse.userservice.domain.request.ServiceRequest;
import com.hse.userservice.domain.request.ServiceRequestStatus;
import com.hse.userservice.domain.user.User;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.exception.ResourceNotFoundException;
import com.hse.userservice.internal.dto.*;
import com.hse.userservice.repository.*;
import com.hse.userservice.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOperationsInternalService {
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayRequestRepository payRequestRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MessageRepository messageRepository;
    private final BalanceService balanceService;

    public List<CoworkingUserReadModelDto> getUsers(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, User> users = usersById(memberships);
        Map<Long, Long> balances = balanceService.getBalancesMinorUnits(membershipIds(memberships));
        return memberships.stream().map(membership -> new CoworkingUserReadModelDto(
                membership.getUserId(),
                users.get(membership.getUserId()).getName(),
                membership.getCreatedAt().toLocalDate(),
                toInt(balances.getOrDefault(membership.getId(), 0L)),
                null,
                null
        )).toList();
    }

    public UserQueueSummaryDto getSummary(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        List<Long> membershipIds = membershipIds(memberships);
        return new UserQueueSummaryDto(
                memberships.size(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.PENDING).count(),
                (int) payRequestRepository.countByMembershipIdInAndStatus(membershipIds, PayRequestStatus.PENDING),
                (int) serviceRequestRepository.countByMembershipIdInAndStatusNotIn(
                        membershipIds,
                        List.of(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.REJECTED)
                ),
                null,
                null,
                null,
                null,
                null
        );
    }

    public UserAnalyticsDto getAnalytics(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        List<Long> membershipIds = membershipIds(memberships);
        return new UserAnalyticsDto(
                memberships.size(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.ACTIVE).count(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.PENDING).count(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.BLOCKED).count(),
                null,
                null,
                (int) serviceRequestRepository.countByMembershipIdInAndStatusNotIn(
                        membershipIds,
                        List.of(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.REJECTED)
                ),
                (int) payRequestRepository.countByMembershipIdInAndStatus(membershipIds, PayRequestStatus.PENDING),
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }

    public List<MembershipQueueItemDto> getMemberships(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, User> users = usersById(memberships);
        return memberships.stream().map(item -> new MembershipQueueItemDto(
                item.getId(),
                item.getUserId(),
                users.get(item.getUserId()).getName(),
                null,
                item.getStatus().name().toLowerCase(),
                item.getCreatedAt().toLocalDate()
        )).toList();
    }

    public List<PayRequestQueueItemDto> getPayRequests(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, Membership> membershipById = memberships.stream().collect(Collectors.toMap(
                Membership::getId,
                Function.identity()
        ));
        Map<Long, User> users = usersById(memberships);
        return payRequestRepository.findAllByMembershipIdInOrderByCreatedAtDesc(membershipIds(memberships))
                .stream()
                .map(item -> {
                    Membership membership = membershipById.get(item.getMembershipId());
                    User user = users.get(membership.getUserId());
                    return new PayRequestQueueItemDto(
                            item.getId(),
                            item.getMembershipId(),
                            membership.getUserId(),
                            user.getName(),
                            toInt(item.getAmount()),
                            item.getStatus().name(),
                            item.getUserComment(),
                            item.getAdminComment(),
                            item.getCreatedAt().toLocalDate()
                    );
                })
                .toList();
    }

    public List<ServiceRequestQueueItemDto> getServiceRequests(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, Membership> membershipById = memberships.stream().collect(Collectors.toMap(
                Membership::getId,
                Function.identity()
        ));
        Map<Long, User> users = usersById(memberships);
        return serviceRequestRepository.findAllByMembershipIdInOrderByCreatedAtDesc(membershipIds(memberships))
                .stream()
                .map(item -> {
                    Membership membership = membershipById.get(item.getMembershipId());
                    User user = users.get(membership.getUserId());
                    return new ServiceRequestQueueItemDto(
                            item.getId(),
                            item.getMembershipId(),
                            membership.getUserId(),
                            user.getName(),
                            "Type #" + item.getTypeId(),
                            item.getName(),
                            item.getCost(),
                            item.getStatus().name().toLowerCase(),
                            item.getCreatedAt().toLocalDate()
                    );
                })
                .toList();
    }

    public InternalServiceRequestDetailDto getServiceRequestDetails(Long coworkingId, Long serviceRequestId) {
        ServiceRequest request = getServiceRequestForCoworking(coworkingId, serviceRequestId);
        Membership membership = getMembershipForCoworking(coworkingId, request.getMembershipId());
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "User not found for service request."));
        return new InternalServiceRequestDetailDto(
                request.getId(),
                request.getMembershipId(),
                membership.getUserId(),
                user.getName(),
                null,
                "Type #" + request.getTypeId(),
                request.getName(),
                request.getCost(),
                null,
                request.getStatus().name().toLowerCase(),
                request.getCreatedAt(),
                request.getCreatedAt(),
                request.getResolvedAt()
        );
    }

    public List<InternalServiceRequestMessageDto> getServiceRequestMessages(Long coworkingId, Long serviceRequestId) {
        ServiceRequest request = getServiceRequestForCoworking(coworkingId, serviceRequestId);
        Membership membership = getMembershipForCoworking(coworkingId, request.getMembershipId());
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "User not found for service request."));
        return messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(request.getId())
                .stream()
                .map(message -> toInternalMessageDto(message, user))
                .toList();
    }

    @Transactional
    public InternalServiceRequestMessageDto addAdminServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            CreateInternalServiceRequestMessageDto dto
    ) {
        ServiceRequest request = getServiceRequestForCoworking(coworkingId, serviceRequestId);
        if (request.getStatus() == ServiceRequestStatus.RESOLVED || request.getStatus() == ServiceRequestStatus.REJECTED) {
            throw new ResourceConflictException("Cannot add message to final service request.");
        }
        Membership membership = getMembershipForCoworking(coworkingId, request.getMembershipId());
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "User not found for service request."));

        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.ADMIN);
        message.setAuthorId(null);
        message.setText(dto.text().trim());
        return toInternalMessageDto(messageRepository.save(message), user);
    }

    @Transactional
    public void approveMembership(Long coworkingId, Long membershipId) {
        Membership membership = getMembershipForCoworking(coworkingId, membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResourceConflictException("Membership can be approved only from pending status.");
        }
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setApprovedAt(LocalDateTime.now());
        membership.setBlockedAt(null);
        membershipRepository.save(membership);
    }

    @Transactional
    public void rejectMembership(Long coworkingId, Long membershipId) {
        Membership membership = getMembershipForCoworking(coworkingId, membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResourceConflictException("Membership can be rejected only from pending status.");
        }
        membership.setStatus(MembershipStatus.BLOCKED);
        membership.setBlockedAt(LocalDateTime.now());
        membershipRepository.save(membership);
    }

    @Transactional
    public void approvePayRequest(Long coworkingId, Long payRequestId) {
        PayRequest payRequest = getPayRequestForCoworking(coworkingId, payRequestId);
        if (payRequest.getStatus() != PayRequestStatus.PENDING) {
            throw new ResourceConflictException("Pay request can be approved only from Pending status.");
        }
        Membership membership = getMembershipForCoworking(coworkingId, payRequest.getMembershipId());
        long resultingBalance = balanceService.getBalanceMinorUnits(membership.getId()) + payRequest.getAmount();
        if (resultingBalance < 0) {
            throw new ResourceConflictException("Pay request approval would make balance negative.");
        }

        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(membership.getId());
        entry.setCoworkingId(coworkingId);
        entry.setAmount(payRequest.getAmount());
        entry.setType(payRequest.getAmount() >= 0 ? LedgerEntryType.DEPOSIT : LedgerEntryType.WITHDRAWAL);
        entry.setName(payRequest.getAmount() >= 0 ? "Пополнение счёта" : "Списание со счёта");
        entry.setComment(payRequest.getUserComment());
        entry.setTimestamp(LocalDateTime.now());
        ledgerEntryRepository.save(entry);

        payRequest.setStatus(PayRequestStatus.APPROVED);
        payRequest.setAdminComment("Проверено, платёж пришел");
        payRequestRepository.save(payRequest);
    }

    @Transactional
    public void rejectPayRequest(Long coworkingId, Long payRequestId) {
        PayRequest payRequest = getPayRequestForCoworking(coworkingId, payRequestId);
        if (payRequest.getStatus() != PayRequestStatus.PENDING) {
            throw new ResourceConflictException("Pay request can be rejected only from Pending status.");
        }
        payRequest.setStatus(PayRequestStatus.REJECTED);
        payRequest.setAdminComment("Платёж не был получен");
        payRequestRepository.save(payRequest);
    }

    @Transactional
    public void advanceServiceRequest(Long coworkingId, Long serviceRequestId, String status) {
        ServiceRequest request = getServiceRequestForCoworking(coworkingId, serviceRequestId);
        ServiceRequestStatus previousStatus = request.getStatus();
        ServiceRequestStatus target;
        try {
            target = ServiceRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported service request status: " + status);
        }
        if (previousStatus == ServiceRequestStatus.RESOLVED || previousStatus == ServiceRequestStatus.REJECTED) {
            throw new ResourceConflictException("Final service request statuses cannot be changed.");
        }
        if (previousStatus == target) {
            return;
        }

        if (target == ServiceRequestStatus.RESOLVED) {
            if (request.getCost() > 0) {
                long resultingBalance = balanceService.getBalanceMinorUnits(request.getMembershipId()) - request.getCost();
                if (resultingBalance < 0) {
                    throw new ResourceConflictException(
                            "Service request cannot be closed because the user balance is insufficient.");
                }

                LedgerEntry entry = new LedgerEntry();
                entry.setMembershipId(request.getMembershipId());
                entry.setCoworkingId(coworkingId);
                entry.setAmount(-request.getCost().longValue());
                entry.setType(LedgerEntryType.SERVICE_REQUEST_CHARGE);
                entry.setName("Списание за сервисную заявку");
                entry.setComment(request.getName());
                entry.setTimestamp(LocalDateTime.now());
                ledgerEntryRepository.save(entry);
            }

            request.setResolvedAt(LocalDateTime.now());
        }

        request.setStatus(target);
        serviceRequestRepository.save(request);
        addServiceRequestStatusChangedMessage(request.getId(), previousStatus, target);
    }

    private void addServiceRequestStatusChangedMessage(
            Long serviceRequestId,
            ServiceRequestStatus previousStatus,
            ServiceRequestStatus targetStatus
    ) {
        Message message = new Message();
        message.setServiceRequestId(serviceRequestId);
        message.setAuthorType(MessageAuthorType.SYSTEM);
        message.setAuthorId(null);
        message.setText("Статус заявки изменён: " + serviceRequestStatusDisplayName(previousStatus) + " → " + serviceRequestStatusDisplayName(
                targetStatus) + ".");
        messageRepository.save(message);
    }

    private String serviceRequestStatusDisplayName(ServiceRequestStatus status) {
        return switch (status) {
            case NEW -> "новая";
            case IN_PROGRESS -> "в работе";
            case RESOLVED -> "выполнена";
            case REJECTED -> "отклонена";
        };
    }

    private Membership getMembershipForCoworking(Long coworkingId, Long membershipId) {
        return membershipRepository.findById(membershipId)
                .filter(item -> item.getCoworkingId().equals(coworkingId))
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for coworking."));
    }

    private PayRequest getPayRequestForCoworking(Long coworkingId, Long payRequestId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        return payRequestRepository.findByIdAndMembershipIdIn(payRequestId, membershipIds(memberships))
                .orElseThrow(() -> new ResourceNotFoundException("Pay request not found for coworking."));
    }

    private ServiceRequest getServiceRequestForCoworking(Long coworkingId, Long serviceRequestId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        return serviceRequestRepository.findByIdAndMembershipIdIn(serviceRequestId, membershipIds(memberships))
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found for coworking."));
    }

    private Map<Long, User> usersById(List<Membership> memberships) {
        List<Long> userIds = memberships.stream().map(Membership::getUserId).distinct().toList();
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private List<Long> membershipIds(List<Membership> memberships) {
        return memberships.stream().map(Membership::getId).toList();
    }

    private InternalServiceRequestMessageDto toInternalMessageDto(Message message, User user) {
        return new InternalServiceRequestMessageDto(
                message.getId(),
                message.getServiceRequestId(),
                message.getAuthorType().name(),
                switch (message.getAuthorType()) {
                    case USER -> user.getName();
                    case ADMIN -> "Администратор";
                    case SYSTEM -> "Система";
                },
                message.getText(),
                message.getTimestamp(),
                null
        );
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
