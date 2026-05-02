package com.hse.userservice.internal.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.domain.booking.Booking;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOperationsInternalService {
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayRequestRepository payRequestRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MessageRepository messageRepository;
    private final BalanceService balanceService;
    private final AdminServiceClient adminServiceClient;

    public List<CoworkingUserReadModelDto> getUsers(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, User> users = usersById(memberships);
        Map<Long, Long> balances = balanceService.getBalancesMinorUnits(membershipIds(memberships));
        Map<Long, Integer> totalBookings = bookingCountByMembership(memberships, false);
        Map<Long, Integer> unfinishedBookings = bookingCountByMembership(memberships, true);

        return memberships.stream().map(membership -> new CoworkingUserReadModelDto(
                membership.getUserId(),
                users.get(membership.getUserId()).getName(),
                membership.getCreatedAt().toLocalDate(),
                toInt(balances.getOrDefault(membership.getId(), 0L)),
                totalBookings.getOrDefault(membership.getId(), 0),
                unfinishedBookings.getOrDefault(membership.getId(), 0)
        )).toList();
    }

    public UserQueueSummaryDto getSummary(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        List<Long> membershipIds = membershipIds(memberships);
        int currentBalance = toInt(balanceService.getBalancesMinorUnits(membershipIds)
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum());
        int monthlyIncome = currentMonthIncome(coworkingId);
        return new UserQueueSummaryDto(
                memberships.size(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.PENDING).count(),
                (int) payRequestRepository.countByMembershipIdInAndStatus(membershipIds, PayRequestStatus.PENDING),
                (int) serviceRequestRepository.countByMembershipIdInAndStatusNotIn(
                        membershipIds,
                        List.of(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.REJECTED)
                ),
                (int) bookingRepository.countByMembershipIdIn(membershipIds),
                (int) bookingRepository.countByMembershipIdInAndActiveTrue(membershipIds),
                currentBalance,
                monthlyIncome,
                calculateMonthlyOccupancyPercent(coworkingId)
        );
    }

    public UserAnalyticsDto getAnalytics(Long coworkingId) {
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        List<Long> membershipIds = membershipIds(memberships);
        int totalBalance = toInt(balanceService.getBalancesMinorUnits(membershipIds)
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum());
        return new UserAnalyticsDto(
                memberships.size(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.ACTIVE).count(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.PENDING).count(),
                (int) memberships.stream().filter(item -> item.getStatus() == MembershipStatus.BLOCKED).count(),
                (int) bookingRepository.countByMembershipIdIn(membershipIds),
                (int) bookingRepository.countByMembershipIdInAndActiveTrue(membershipIds),
                (int) serviceRequestRepository.countByMembershipIdInAndStatusNotIn(
                        membershipIds,
                        List.of(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.REJECTED)
                ),
                (int) payRequestRepository.countByMembershipIdInAndStatus(membershipIds, PayRequestStatus.PENDING),
                totalBalance,
                currentMonthIncome(coworkingId),
                calculateMonthlyOccupancyPercent(coworkingId),
                monthlyIncomeHistory(coworkingId),
                occupancyHistory(coworkingId)
        );
    }

    public List<MembershipQueueItemDto> getMemberships(Long coworkingId) {
        String coworkingName = adminServiceClient.getCoworkingInfo(coworkingId).name();
        List<Membership> memberships = membershipRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId);
        Map<Long, User> users = usersById(memberships);
        return memberships.stream().map(item -> new MembershipQueueItemDto(
                item.getId(),
                item.getUserId(),
                users.get(item.getUserId()).getName(),
                coworkingName,
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
        Map<Long, String> requestTypeNames = requestTypeNames(coworkingId);
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
                            requestTypeNames.getOrDefault(item.getTypeId(), "Unknown type"),
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
        String typeName = requestTypeNames(coworkingId).getOrDefault(
                request.getTypeId(),
                "Type #" + request.getTypeId()
        );
        LocalDateTime updatedAt = messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(request.getId())
                .stream()
                .map(Message::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(request.getResolvedAt() == null ? request.getCreatedAt() : request.getResolvedAt());

        return new InternalServiceRequestDetailDto(
                request.getId(),
                request.getMembershipId(),
                membership.getUserId(),
                user.getName(),
                user.getEmail(),
                typeName,
                request.getName(),
                request.getCost(),
                balanceService.getBalanceMinorUnits(membership.getId()),
                request.getStatus().name().toLowerCase(),
                request.getCreatedAt(),
                updatedAt,
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

    private Map<Long, Integer> bookingCountByMembership(List<Membership> memberships, boolean onlyActive) {
        return bookingRepository.findAllByMembershipIdInOrderByDateDesc(membershipIds(memberships))
                .stream()
                .filter(item -> !onlyActive || Boolean.TRUE.equals(item.getActive()))
                .collect(Collectors.groupingBy(
                        Booking::getMembershipId,
                        Collectors.reducing(0, item -> 1, Integer::sum)
                ));
    }

    private int currentMonthIncome(Long coworkingId) {
        YearMonth month = YearMonth.now();
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return toInt(ledgerEntryRepository.findAllByCoworkingIdAndTimestampBetweenOrderByTimestampAsc(
                coworkingId,
                from,
                to
        ).stream().filter(item -> EnumSet.of(LedgerEntryType.BOOKING_CHARGE, LedgerEntryType.SERVICE_REQUEST_CHARGE)
                .contains(item.getType())).mapToLong(item -> Math.abs(item.getAmount())).sum());
    }

    private int calculateMonthlyOccupancyPercent(Long coworkingId) {
        LocalDate now = LocalDate.now();
        long currentMonthBookings = bookingRepository.countByCoworkingIdAndActiveTrueAndDateBetween(
                coworkingId,
                now.withDayOfMonth(1),
                now.withDayOfMonth(now.lengthOfMonth())
        );
        return (int) Math.min(100, currentMonthBookings * 8);
    }

    private List<AnalyticsMetricPointDto> monthlyIncomeHistory(Long coworkingId) {
        return IntStream.rangeClosed(0, 5).mapToObj(offset -> YearMonth.now().minusMonths(5 - offset)).map(month -> {
            LocalDateTime from = month.atDay(1).atStartOfDay();
            LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
            int value = toInt(ledgerEntryRepository.findAllByCoworkingIdAndTimestampBetweenOrderByTimestampAsc(
                    coworkingId,
                    from,
                    to
            ).stream().filter(item -> EnumSet.of(LedgerEntryType.BOOKING_CHARGE, LedgerEntryType.SERVICE_REQUEST_CHARGE)
                    .contains(item.getType())).mapToLong(item -> Math.abs(item.getAmount())).sum());
            return new AnalyticsMetricPointDto(month.getMonth().name().substring(0, 3), value);
        }).toList();
    }

    private List<AnalyticsMetricPointDto> occupancyHistory(Long coworkingId) {
        return IntStream.rangeClosed(0, 5).mapToObj(offset -> YearMonth.now().minusMonths(5 - offset)).map(month -> {
            long bookings = bookingRepository.countByCoworkingIdAndActiveTrueAndDateBetween(
                    coworkingId,
                    month.atDay(1),
                    month.atEndOfMonth()
            );
            return new AnalyticsMetricPointDto(
                    month.getMonth().name().substring(0, 3),
                    (int) Math.min(100, bookings * 8)
            );
        }).toList();
    }

    private Map<Long, String> requestTypeNames(Long coworkingId) {
        return adminServiceClient.getCoworkingConfigSnapshot(coworkingId).serviceRequestTypes().stream().collect(
                Collectors.toMap(
                        com.hse.userservice.client.dto.CoworkingConfigSnapshot.ServiceRequestType::id,
                        com.hse.userservice.client.dto.CoworkingConfigSnapshot.ServiceRequestType::name
                ));
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
                message.getReadAt()
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
