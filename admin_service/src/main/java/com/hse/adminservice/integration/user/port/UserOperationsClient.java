package com.hse.adminservice.integration.user.port;

import com.hse.adminservice.operations.analytics.dto.UserAnalyticsResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.UserQueueSummaryResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestDetailResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;

import java.util.List;

public interface UserOperationsClient {
    List<CoworkingUserReadModelResponse> getUsers(Long coworkingId);

    UserQueueSummaryResponse getSummary(Long coworkingId);

    UserAnalyticsResponse getAnalytics(Long coworkingId);

    List<MembershipQueueItemResponse> getMemberships(Long coworkingId);

    List<PayRequestQueueItemResponse> getPayRequests(Long coworkingId);

    List<ServiceRequestQueueItemResponse> getServiceRequests(Long coworkingId);

    ServiceRequestDetailResponse getServiceRequestDetails(Long coworkingId, Long serviceRequestId);

    List<ServiceRequestMessageResponse> getServiceRequestMessages(Long coworkingId, Long serviceRequestId);

    ServiceRequestMessageResponse addServiceRequestMessage(Long coworkingId, Long serviceRequestId, String text);

    void approveMembership(Long coworkingId, Long membershipId);

    void rejectMembership(Long coworkingId, Long membershipId);

    void approvePayRequest(Long coworkingId, Long payRequestId);

    void rejectPayRequest(Long coworkingId, Long payRequestId);

    void advanceServiceRequest(Long coworkingId, Long serviceRequestId, String status);
}
