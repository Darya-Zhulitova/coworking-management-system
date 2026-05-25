package com.hse.adminservice.integration.user.port;

import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.operations.common.DecisionRequest;
import com.hse.adminservice.operations.dashboard.OperationsDashboardResponse;
import com.hse.adminservice.operations.membership.dto.ManualBalanceAdjustmentRequest;
import com.hse.adminservice.operations.membership.dto.MembershipListItemResponse;
import com.hse.adminservice.operations.membership.dto.MembershipProfileResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestWorkspaceResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserOperationsClient {
    PlaceBookingListResponse getPlaceBookings(Long coworkingId, Long placeId);

    List<CoworkingUserReadModelResponse> getUsers(Long coworkingId);

    List<MembershipListItemResponse> getMembershipList(Long coworkingId, String search);

    MembershipProfileResponse getMembershipProfile(Long coworkingId, Long membershipId);

    MembershipProfileResponse adjustMembershipBalance(
            Long coworkingId,
            Long membershipId,
            ManualBalanceAdjustmentRequest request
    );

    OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId);

    OperationalImpactResponse blockMembership(Long coworkingId, Long membershipId, ImpactCommitRequest request);

    OperationsDashboardResponse getOperationsDashboard(Long coworkingId);

    List<MembershipQueueItemResponse> getMemberships(Long coworkingId);

    void decideMembership(Long coworkingId, Long membershipId, DecisionRequest request);

    List<PayRequestQueueItemResponse> getPayRequests(Long coworkingId);

    void decidePayRequest(Long coworkingId, Long payRequestId, DecisionRequest request);

    List<ServiceRequestQueueItemResponse> getServiceRequests(Long coworkingId);

    ServiceRequestWorkspaceResponse getServiceRequestWorkspace(Long coworkingId, Long serviceRequestId);

    ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    );

    void decideServiceRequest(Long coworkingId, Long serviceRequestId, DecisionRequest request);
}
