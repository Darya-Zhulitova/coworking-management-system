package com.hse.adminservice.coworking.service;

import com.hse.adminservice.coworking.dto.*;
import com.hse.adminservice.integration.user.client.UserOperationsHttpClient;
import com.hse.adminservice.place.repository.PlaceRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.entity.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class CoworkingReadModelService {
    private final AdminAuthorizationService authorizationService;
    private final PlaceRepository placeRepository;
    private final UserOperationsHttpClient userOperationsClient;

    public List<CoworkingUserReadModelResponse> getUsers(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getUsers(coworkingId);
    }

    public UserQueueSummaryResponse getUserQueueSummary(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getSummary(coworkingId);
    }

    public UserAnalyticsResponse getUserAnalytics(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getAnalytics(coworkingId);
    }

    public List<MembershipQueueItemResponse> getMembershipQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getMemberships(coworkingId);
    }

    public List<PayRequestQueueItemResponse> getPayRequestQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getPayRequests(coworkingId);
    }

    public List<ServiceRequestQueueItemResponse> getServiceRequestQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getServiceRequests(coworkingId);
    }

    public ServiceRequestDetailResponse getServiceRequestDetails(Long coworkingId, Long serviceRequestId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getServiceRequestDetails(coworkingId, serviceRequestId);
    }

    public List<ServiceRequestMessageResponse> getServiceRequestMessages(Long coworkingId, Long serviceRequestId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getServiceRequestMessages(coworkingId, serviceRequestId);
    }

    public ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        return userOperationsClient.addServiceRequestMessage(coworkingId, serviceRequestId, text);
    }

    public void approveMembership(Long coworkingId, Long membershipId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.approveMembership(coworkingId, membershipId);
    }

    public void rejectMembership(Long coworkingId, Long membershipId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.rejectMembership(coworkingId, membershipId);
    }

    public void approvePayRequest(Long coworkingId, Long payRequestId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.approvePayRequest(coworkingId, payRequestId);
    }

    public void rejectPayRequest(Long coworkingId, Long payRequestId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.rejectPayRequest(coworkingId, payRequestId);
    }

    public void advanceServiceRequest(Long coworkingId, Long requestId, String status) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.advanceServiceRequest(coworkingId, requestId, status);
    }

    public List<PlaceOperationalResponse> getOperationalPlaces(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.BOOKING_READ);
        AtomicInteger n = new AtomicInteger(1);
        return placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .filter(place -> floorId == null || place.getFloor().getId().equals(floorId))
                .map(place -> {
                    int i = n.getAndIncrement();
                    return PlaceOperationalResponse.builder()
                            .placeId(place.getId())
                            .placeName(place.getName())
                            .placeTypeName(place.getPlaceType().getName())
                            .totalBookings(i * 3)
                            .unfinishedBookings(i % 3)
                            .active(place.getActive())
                            .floorId(place.getFloor().getId())
                            .build();
                })
                .toList();
    }
}
