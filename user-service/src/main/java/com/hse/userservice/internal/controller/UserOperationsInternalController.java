package com.hse.userservice.internal.controller;

import com.hse.userservice.internal.dto.*;
import com.hse.userservice.internal.service.UserOperationsInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/coworkings/{coworkingId}/users")
@RequiredArgsConstructor
public class UserOperationsInternalController {
    private final UserOperationsInternalService service;

    @GetMapping
    public List<CoworkingUserReadModelDto> getUsers(@PathVariable Long coworkingId) {
        return service.getUsers(coworkingId);
    }

    @GetMapping("/summary")
    public UserQueueSummaryDto getSummary(@PathVariable Long coworkingId) {
        return service.getSummary(coworkingId);
    }

    @GetMapping("/analytics")
    public UserAnalyticsDto getAnalytics(@PathVariable Long coworkingId) {
        return service.getAnalytics(coworkingId);
    }

    @GetMapping("/memberships")
    public List<MembershipQueueItemDto> getMemberships(@PathVariable Long coworkingId) {
        return service.getMemberships(coworkingId);
    }

    @PostMapping("/memberships/{membershipId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveMembership(@PathVariable Long coworkingId, @PathVariable Long membershipId) {
        service.approveMembership(coworkingId, membershipId);
    }

    @PostMapping("/memberships/{membershipId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectMembership(@PathVariable Long coworkingId, @PathVariable Long membershipId) {
        service.rejectMembership(coworkingId, membershipId);
    }

    @GetMapping("/pay-requests")
    public List<PayRequestQueueItemDto> getPayRequests(@PathVariable Long coworkingId) {
        return service.getPayRequests(coworkingId);
    }

    @PostMapping("/pay-requests/{payRequestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePayRequest(@PathVariable Long coworkingId, @PathVariable Long payRequestId) {
        service.approvePayRequest(coworkingId, payRequestId);
    }

    @PostMapping("/pay-requests/{payRequestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPayRequest(@PathVariable Long coworkingId, @PathVariable Long payRequestId) {
        service.rejectPayRequest(coworkingId, payRequestId);
    }

    @GetMapping("/service-requests")
    public List<ServiceRequestQueueItemDto> getServiceRequests(@PathVariable Long coworkingId) {
        return service.getServiceRequests(coworkingId);
    }

    @GetMapping("/service-requests/{serviceRequestId}")
    public InternalServiceRequestDetailDto getServiceRequestDetails(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return service.getServiceRequestDetails(coworkingId, serviceRequestId);
    }

    @GetMapping("/service-requests/{serviceRequestId}/messages")
    public List<InternalServiceRequestMessageDto> getServiceRequestMessages(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId
    ) {
        return service.getServiceRequestMessages(coworkingId, serviceRequestId);
    }

    @PostMapping("/service-requests/{serviceRequestId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public InternalServiceRequestMessageDto addServiceRequestMessage(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @Valid @RequestBody CreateInternalServiceRequestMessageDto dto
    ) {
        return service.addAdminServiceRequestMessage(coworkingId, serviceRequestId, dto);
    }

    @PostMapping("/service-requests/{serviceRequestId}/status/{status}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void advanceServiceRequest(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestId,
            @PathVariable String status
    ) {
        service.advanceServiceRequest(coworkingId, serviceRequestId, status);
    }
}
