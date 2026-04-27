package com.hse.adminservice.operations.api;

import com.hse.adminservice.operations.analytics.dto.UserAnalyticsResponse;
import com.hse.adminservice.operations.application.CoworkingReadModelService;
import com.hse.adminservice.operations.bookingimpact.dto.PlaceOperationalResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.UserQueueSummaryResponse;
import com.hse.adminservice.operations.servicedesk.dto.CreateServiceRequestMessageRequest;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestDetailResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}")
@RequiredArgsConstructor
public class CoworkingReadModelController {
    private final CoworkingReadModelService readModelService;

    @GetMapping("/users")
    public List<CoworkingUserReadModelResponse> getUsers(@PathVariable Long coworkingId) {
        return readModelService.getUsers(coworkingId);
    }

    @GetMapping("/users/summary")
    public UserQueueSummaryResponse getUserQueueSummary(@PathVariable Long coworkingId) {
        return readModelService.getUserQueueSummary(coworkingId);
    }

    @GetMapping("/users/analytics")
    public UserAnalyticsResponse getUserAnalytics(@PathVariable Long coworkingId) {
        return readModelService.getUserAnalytics(coworkingId);
    }

    @GetMapping("/users/memberships")
    public List<MembershipQueueItemResponse> getMembershipQueue(@PathVariable Long coworkingId) {
        return readModelService.getMembershipQueue(coworkingId);
    }

    @PostMapping("/users/memberships/{membershipId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveMembership(@PathVariable Long coworkingId, @PathVariable Long membershipId) {
        readModelService.approveMembership(coworkingId, membershipId);
    }

    @PostMapping("/users/memberships/{membershipId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectMembership(@PathVariable Long coworkingId, @PathVariable Long membershipId) {
        readModelService.rejectMembership(coworkingId, membershipId);
    }

    @GetMapping("/users/pay-requests")
    public List<PayRequestQueueItemResponse> getPayRequestQueue(@PathVariable Long coworkingId) {
        return readModelService.getPayRequestQueue(coworkingId);
    }

    @PostMapping("/users/pay-requests/{payRequestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePayRequest(@PathVariable Long coworkingId, @PathVariable Long payRequestId) {
        readModelService.approvePayRequest(coworkingId, payRequestId);
    }

    @PostMapping("/users/pay-requests/{payRequestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPayRequest(@PathVariable Long coworkingId, @PathVariable Long payRequestId) {
        readModelService.rejectPayRequest(coworkingId, payRequestId);
    }

    @GetMapping("/users/service-requests")
    public List<ServiceRequestQueueItemResponse> getServiceRequestQueue(@PathVariable Long coworkingId) {
        return readModelService.getServiceRequestQueue(coworkingId);
    }

    @GetMapping("/users/service-requests/{serviceRequestId}")
    public ServiceRequestDetailResponse getServiceRequestDetails(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return readModelService.getServiceRequestDetails(coworkingId, serviceRequestId);
    }

    @GetMapping("/users/service-requests/{serviceRequestId}/messages")
    public List<ServiceRequestMessageResponse> getServiceRequestMessages(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return readModelService.getServiceRequestMessages(coworkingId, serviceRequestId);
    }

    @PostMapping("/users/service-requests/{serviceRequestId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestMessageResponse addServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @RequestBody @jakarta.validation.Valid CreateServiceRequestMessageRequest request
    ) {
        return readModelService.addServiceRequestMessage(coworkingId, serviceRequestId, request.getText().trim());
    }

    @PostMapping("/users/service-requests/{serviceRequestId}/status/{status}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void advanceServiceRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @PathVariable String status
    ) {
        readModelService.advanceServiceRequest(coworkingId, serviceRequestId, status);
    }

    @GetMapping("/places/operational")
    public List<PlaceOperationalResponse> getOperationalPlaces(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) Long floorId
    ) {
        return readModelService.getOperationalPlaces(coworkingId, floorId);
    }
}
