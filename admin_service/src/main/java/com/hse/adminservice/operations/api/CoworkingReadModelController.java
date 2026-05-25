package com.hse.adminservice.operations.api;

import com.hse.adminservice.operations.application.CoworkingReadModelService;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.operations.bookingimpact.dto.PlaceOperationalResponse;
import com.hse.adminservice.operations.common.DecisionRequest;
import com.hse.adminservice.operations.dashboard.OperationsDashboardResponse;
import com.hse.adminservice.operations.membership.dto.ManualBalanceAdjustmentRequest;
import com.hse.adminservice.operations.membership.dto.MembershipListItemResponse;
import com.hse.adminservice.operations.membership.dto.MembershipProfileResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.servicedesk.dto.CreateServiceRequestMessageRequest;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestWorkspaceResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/memberships")
    public List<MembershipListItemResponse> getMembershipList(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) String search
    ) {
        return readModelService.getMembershipList(coworkingId, search);
    }

    @GetMapping("/memberships/{membershipId}")
    public MembershipProfileResponse getMembershipProfile(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId
    ) {
        return readModelService.getMembershipProfile(coworkingId, membershipId);
    }

    @PostMapping("/memberships/{membershipId}/balance-adjustments")
    public MembershipProfileResponse adjustMembershipBalance(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @Valid @RequestBody ManualBalanceAdjustmentRequest request
    ) {
        return readModelService.adjustMembershipBalance(coworkingId, membershipId, request);
    }


    @PostMapping("/memberships/{membershipId}/block-preview")
    public OperationalImpactResponse previewMembershipBlock(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId
    ) {
        return readModelService.previewMembershipBlock(coworkingId, membershipId);
    }

    @PostMapping("/memberships/{membershipId}/block")
    public OperationalImpactResponse blockMembership(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @RequestBody @Valid ImpactCommitRequest request
    ) {
        return readModelService.blockMembership(coworkingId, membershipId, request);
    }

    @GetMapping("/operations-dashboard")
    public OperationsDashboardResponse getOperationsDashboard(@PathVariable Long coworkingId) {
        return readModelService.getOperationsDashboard(coworkingId);
    }

    @GetMapping("/membership-requests")
    public List<MembershipQueueItemResponse> getMembershipQueue(@PathVariable Long coworkingId) {
        return readModelService.getMembershipQueue(coworkingId);
    }

    @PostMapping("/membership-requests/{membershipId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideMembership(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @Valid @RequestBody DecisionRequest request
    ) {
        readModelService.decideMembership(coworkingId, membershipId, request);
    }

    @GetMapping("/pay-requests")
    public List<PayRequestQueueItemResponse> getPayRequestQueue(@PathVariable Long coworkingId) {
        return readModelService.getPayRequestQueue(coworkingId);
    }

    @PostMapping("/pay-requests/{payRequestId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decidePayRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long payRequestId,
            @Valid @RequestBody DecisionRequest request
    ) {
        readModelService.decidePayRequest(coworkingId, payRequestId, request);
    }

    @GetMapping("/service-requests")
    public List<ServiceRequestQueueItemResponse> getServiceRequestQueue(@PathVariable Long coworkingId) {
        return readModelService.getServiceRequestQueue(coworkingId);
    }

    @GetMapping("/service-requests/{serviceRequestId}/workspace")
    public ServiceRequestWorkspaceResponse getServiceRequestWorkspace(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return readModelService.getServiceRequestWorkspace(coworkingId, serviceRequestId);
    }

    @PostMapping(value = "/service-requests/{serviceRequestId}/messages", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestMessageResponse addServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) MultipartFile file
    ) {
        return readModelService.addServiceRequestMessage(coworkingId, serviceRequestId, text, file);
    }

    @PostMapping("/service-requests/{serviceRequestId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestMessageResponse addJsonServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @RequestBody @Valid CreateServiceRequestMessageRequest request
    ) {
        return readModelService.addServiceRequestMessage(coworkingId, serviceRequestId, request.getText(), null);
    }

    @PostMapping("/service-requests/{serviceRequestId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideServiceRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @Valid @RequestBody DecisionRequest request
    ) {
        readModelService.decideServiceRequest(coworkingId, serviceRequestId, request);
    }

    @GetMapping("/places/operational")
    public List<PlaceOperationalResponse> getOperationalPlaces(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) Long floorId
    ) {
        return readModelService.getOperationalPlaces(coworkingId, floorId);
    }
}
