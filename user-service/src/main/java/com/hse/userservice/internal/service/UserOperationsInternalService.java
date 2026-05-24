package com.hse.userservice.internal.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.feature.balance.service.BalanceService;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;
import com.hse.userservice.feature.servicerequest.service.ServiceRequestService;
import com.hse.userservice.internal.dto.InternalDecisionRequest;
import com.hse.userservice.internal.dto.OperationsDashboardDto;
import com.hse.userservice.internal.dto.UserAnalyticsDto;
import com.hse.userservice.internal.dto.UserQueueSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOperationsInternalService {
    private final MembershipService membershipService;
    private final BalanceService balanceService;
    private final ServiceRequestService serviceRequestService;

    public UserQueueSummaryDto getSummary(Long coworkingId) {
        MembershipService.AdminMembershipStats stats = membershipService.getAdminStats(coworkingId);
        return new UserQueueSummaryDto(
                stats.total(),
                stats.pending(),
                balanceService.countPendingPayRequestsByCoworking(coworkingId),
                serviceRequestService.countOpenServiceRequestsByCoworking(coworkingId),
                null,
                null,
                null,
                null,
                null
        );
    }

    public UserAnalyticsDto getAnalytics(Long coworkingId) {
        MembershipService.AdminMembershipStats stats = membershipService.getAdminStats(coworkingId);
        return new UserAnalyticsDto(
                stats.total(),
                stats.active(),
                stats.pending(),
                stats.blocked(),
                null,
                null,
                serviceRequestService.countOpenServiceRequestsByCoworking(coworkingId),
                balanceService.countPendingPayRequestsByCoworking(coworkingId),
                membershipService.getTotalBalanceMinorUnits(coworkingId),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    public OperationsDashboardDto getOperationsDashboard(Long coworkingId) {
        MembershipService.AdminMembershipStats stats = membershipService.getAdminStats(coworkingId);
        return new OperationsDashboardDto(
                new OperationsDashboardDto.Queues(
                        stats.pending(),
                        balanceService.countPendingPayRequestsByCoworking(coworkingId),
                        serviceRequestService.countOpenServiceRequestsByCoworking(coworkingId)
                ),
                new OperationsDashboardDto.Memberships(stats.total(), stats.active(), stats.pending(), stats.blocked()),
                new OperationsDashboardDto.Finance(membershipService.getTotalBalanceMinorUnits(coworkingId), 0L),
                new OperationsDashboardDto.Occupancy(0)
        );
    }

    @Transactional
    public void decideMembership(Long coworkingId, Long membershipId, InternalDecisionRequest request) {
        switch (request.decision()) {
            case APPROVE -> membershipService.approveByAdmin(coworkingId, membershipId);
            case REJECT -> membershipService.rejectByAdmin(coworkingId, membershipId);
            default ->
                    throw new ResourceConflictException("Неподдерживаемое решение по пользователю: " + request.decision());
        }
    }

    @Transactional
    public void decidePayRequest(Long coworkingId, Long payRequestId, InternalDecisionRequest request) {
        switch (request.decision()) {
            case APPROVE -> balanceService.approvePayRequestByAdmin(coworkingId, payRequestId, request.comment());
            case REJECT -> balanceService.rejectPayRequestByAdmin(coworkingId, payRequestId, request.comment());
            default ->
                    throw new ResourceConflictException("Неподдерживаемое решение по платежной заявке: " + request.decision());
        }
    }

    @Transactional
    public void decideServiceRequest(Long coworkingId, Long serviceRequestId, InternalDecisionRequest request) {
        ServiceRequestStatus target = switch (request.decision()) {
            case IN_PROGRESS -> ServiceRequestStatus.IN_PROGRESS;
            case RESOLVE, APPROVE -> ServiceRequestStatus.RESOLVED;
            case REJECT -> ServiceRequestStatus.REJECTED;
        };
        serviceRequestService.changeStatusByAdmin(coworkingId, serviceRequestId, target, request.comment());
    }
}
