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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOperationsInternalServiceUnitTest {
    @Mock MembershipService membershipService;
    @Mock BalanceService balanceService;
    @Mock ServiceRequestService serviceRequestService;

    private UserOperationsInternalService service;

    @BeforeEach
    void setUp() {
        service = new UserOperationsInternalService(membershipService, balanceService, serviceRequestService);
    }

    @Test
    void dashboardDelegatesAggregationToDomainServices() {
        when(membershipService.getAdminStats(9L)).thenReturn(new MembershipService.AdminMembershipStats(3, 1, 1, 1));
        when(balanceService.countPendingPayRequestsByCoworking(9L)).thenReturn(4);
        when(serviceRequestService.countOpenServiceRequestsByCoworking(9L)).thenReturn(5);
        when(membershipService.getTotalBalanceMinorUnits(9L)).thenReturn(350L);

        OperationsDashboardDto result = service.getOperationsDashboard(9L);

        assertThat(result.queues().pendingMemberships()).isEqualTo(1);
        assertThat(result.queues().pendingPayRequests()).isEqualTo(4);
        assertThat(result.queues().openServiceRequests()).isEqualTo(5);
        assertThat(result.memberships().total()).isEqualTo(3);
        assertThat(result.memberships().active()).isEqualTo(1);
        assertThat(result.memberships().blocked()).isEqualTo(1);
        assertThat(result.finance().totalBalance()).isEqualTo(350L);
    }

    @Test
    void summaryAndAnalyticsUseDomainServiceCounters() {
        when(membershipService.getAdminStats(9L)).thenReturn(new MembershipService.AdminMembershipStats(10, 7, 2, 1));
        when(balanceService.countPendingPayRequestsByCoworking(9L)).thenReturn(3);
        when(serviceRequestService.countOpenServiceRequestsByCoworking(9L)).thenReturn(4);
        when(membershipService.getTotalBalanceMinorUnits(9L)).thenReturn(8_500L);

        UserQueueSummaryDto summary = service.getSummary(9L);
        UserAnalyticsDto analytics = service.getAnalytics(9L);

        assertThat(summary.usersCount()).isEqualTo(10);
        assertThat(summary.pendingMemberships()).isEqualTo(2);
        assertThat(summary.pendingPayRequests()).isEqualTo(3);
        assertThat(summary.openServiceRequests()).isEqualTo(4);
        assertThat(analytics.usersCount()).isEqualTo(10);
        assertThat(analytics.activeMemberships()).isEqualTo(7);
        assertThat(analytics.totalBalance()).isEqualTo(8_500L);
    }

    @Test
    void decisionsDelegateToDomainServicesAndRejectUnsupportedMembershipDecision() {
        service.decideMembership(9L, 1L, new InternalDecisionRequest(InternalDecisionRequest.Decision.APPROVE, null));
        service.decidePayRequest(9L, 2L, new InternalDecisionRequest(InternalDecisionRequest.Decision.REJECT, "bad receipt"));
        service.decideServiceRequest(9L, 3L, new InternalDecisionRequest(InternalDecisionRequest.Decision.RESOLVE, "done"));

        verify(membershipService).approveByAdmin(9L, 1L);
        verify(balanceService).rejectPayRequestByAdmin(9L, 2L, "bad receipt");
        verify(serviceRequestService).changeStatusByAdmin(9L, 3L, ServiceRequestStatus.RESOLVED, "done");

        assertThatThrownBy(() -> service.decideMembership(
                9L,
                1L,
                new InternalDecisionRequest(InternalDecisionRequest.Decision.IN_PROGRESS, null)
        )).isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Неподдерживаемое решение по пользователю");
    }
}
