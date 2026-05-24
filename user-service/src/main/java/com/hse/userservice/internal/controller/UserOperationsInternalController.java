package com.hse.userservice.internal.controller;

import com.hse.userservice.feature.balance.service.BalanceService;
import com.hse.userservice.feature.booking.service.BookingService;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.servicerequest.service.ServiceRequestService;
import com.hse.userservice.internal.dto.*;
import com.hse.userservice.internal.dto.booking.PlaceBookingListDto;
import com.hse.userservice.internal.dto.membership.InternalMembershipListItemDto;
import com.hse.userservice.internal.dto.membership.InternalMembershipProfileDto;
import com.hse.userservice.internal.dto.membership.ManualBalanceAdjustmentRequest;
import com.hse.userservice.internal.service.UserOperationsInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/internal/coworkings/{coworkingId}")
@RequiredArgsConstructor
public class UserOperationsInternalController {
    private final MembershipService membershipService;
    private final BookingService bookingService;
    private final BalanceService balanceService;
    private final ServiceRequestService serviceRequestService;
    private final UserOperationsInternalService operationsService;

    @GetMapping("/users")
    public List<CoworkingUserReadModelDto> getUsers(@PathVariable Long coworkingId) {
        return membershipService.getCoworkingUsers(coworkingId);
    }

    @GetMapping("/memberships")
    public List<InternalMembershipListItemDto> getMembershipList(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) String search
    ) {
        return membershipService.getInternalMembershipList(coworkingId, search);
    }

    @GetMapping("/memberships/{membershipId}")
    public InternalMembershipProfileDto getMembershipProfile(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId
    ) {
        return membershipService.getInternalMembershipProfile(coworkingId, membershipId);
    }

    @PostMapping("/memberships/{membershipId}/balance-adjustments")
    public InternalMembershipProfileDto adjustMembershipBalance(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @Valid @RequestBody ManualBalanceAdjustmentRequest request
    ) {
        balanceService.adjustBalanceByAdmin(coworkingId, membershipId, request.amountMinorUnits(), request.comment());
        return membershipService.getInternalMembershipProfile(coworkingId, membershipId);
    }

    @GetMapping("/places/{placeId}/bookings")
    public PlaceBookingListDto getCurrentPlaceBookings(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return bookingService.getCurrentPlaceBookings(coworkingId, placeId);
    }

    @GetMapping("/operations-dashboard")
    public OperationsDashboardDto getOperationsDashboard(@PathVariable Long coworkingId) {
        return operationsService.getOperationsDashboard(coworkingId);
    }

    @GetMapping("/membership-requests")
    public List<MembershipQueueItemDto> getMemberships(@PathVariable Long coworkingId) {
        return membershipService.getMembershipQueue(coworkingId);
    }

    @PostMapping("/membership-requests/{membershipId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideMembership(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @Valid @RequestBody InternalDecisionRequest request
    ) {
        operationsService.decideMembership(coworkingId, membershipId, request);
    }

    @PostMapping("/memberships/{membershipId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockMembership(@PathVariable Long coworkingId, @PathVariable Long membershipId) {
        membershipService.blockByAdmin(coworkingId, membershipId);
    }

    @GetMapping("/pay-requests")
    public List<PayRequestQueueItemDto> getPayRequests(@PathVariable Long coworkingId) {
        return balanceService.getPayRequestQueue(coworkingId);
    }

    @PostMapping("/pay-requests/{payRequestId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decidePayRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long payRequestId,
            @Valid @RequestBody InternalDecisionRequest request
    ) {
        operationsService.decidePayRequest(coworkingId, payRequestId, request);
    }

    @GetMapping("/service-requests")
    public List<ServiceRequestQueueItemDto> getServiceRequests(@PathVariable Long coworkingId) {
        return serviceRequestService.getInternalQueue(coworkingId);
    }

    @GetMapping("/service-requests/{serviceRequestId}/workspace")
    public InternalServiceRequestWorkspaceDto getServiceRequestWorkspace(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return serviceRequestService.getInternalWorkspace(coworkingId, serviceRequestId);
    }

    @PostMapping(value = "/service-requests/{serviceRequestId}/messages", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public InternalServiceRequestMessageDto addServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) MultipartFile file
    ) {
        return serviceRequestService.addAdminMessageInternal(coworkingId, serviceRequestId, text, file);
    }

    @PostMapping("/service-requests/{serviceRequestId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public InternalServiceRequestMessageDto addJsonServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @Valid @RequestBody CreateInternalServiceRequestMessageDto dto
    ) {
        return serviceRequestService.addAdminMessageInternal(coworkingId, serviceRequestId, dto.text(), null);
    }

    @PostMapping("/service-requests/{serviceRequestId}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideServiceRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @Valid @RequestBody InternalDecisionRequest request
    ) {
        operationsService.decideServiceRequest(coworkingId, serviceRequestId, request);
    }
}
