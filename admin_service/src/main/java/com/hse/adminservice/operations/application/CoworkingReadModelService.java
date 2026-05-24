package com.hse.adminservice.operations.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.integration.user.port.UserOperationsClient;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.operations.bookingimpact.dto.PlaceOperationalResponse;
import com.hse.adminservice.operations.common.DecisionRequest;
import com.hse.adminservice.operations.dashboard.OperationsDashboardResponse;
import com.hse.adminservice.operations.membership.dto.ManualBalanceAdjustmentRequest;
import com.hse.adminservice.operations.membership.dto.MembershipBookingResponse;
import com.hse.adminservice.operations.membership.dto.MembershipListItemResponse;
import com.hse.adminservice.operations.membership.dto.MembershipProfileResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestWorkspaceResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoworkingReadModelService {
    private final AdminAuthorizationService authorizationService;
    private final PlaceRepository placeRepository;
    private final UserOperationsClient userOperationsClient;

    public List<CoworkingUserReadModelResponse> getUsers(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getUsers(coworkingId);
    }


    public List<MembershipListItemResponse> getMembershipList(Long coworkingId, String search) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getMembershipList(coworkingId, search);
    }

    public MembershipProfileResponse getMembershipProfile(Long coworkingId, Long membershipId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return enrichMembershipProfile(userOperationsClient.getMembershipProfile(coworkingId, membershipId));
    }

    public MembershipProfileResponse adjustMembershipBalance(
            Long coworkingId,
            Long membershipId,
            ManualBalanceAdjustmentRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        return enrichMembershipProfile(userOperationsClient.adjustMembershipBalance(
                coworkingId,
                membershipId,
                request
        ));
    }


    public OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        return enrichImpactResponse(userOperationsClient.previewMembershipBlock(coworkingId, membershipId));
    }

    public OperationalImpactResponse blockMembership(Long coworkingId, Long membershipId, ImpactCommitRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        return enrichImpactResponse(userOperationsClient.blockMembership(coworkingId, membershipId, request));
    }

    public OperationsDashboardResponse getOperationsDashboard(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getOperationsDashboard(coworkingId);
    }

    public List<MembershipQueueItemResponse> getMembershipQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getMemberships(coworkingId);
    }

    public void decideMembership(Long coworkingId, Long membershipId, DecisionRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.decideMembership(coworkingId, membershipId, request);
    }

    public List<PayRequestQueueItemResponse> getPayRequestQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getPayRequests(coworkingId);
    }

    public void decidePayRequest(Long coworkingId, Long payRequestId, DecisionRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        if (request.decision() == DecisionRequest.Decision.REJECT && !StringUtils.hasText(request.comment())) {
            throw new ConflictException("Укажите комментарий администратора при отклонении платежной заявки.");
        }
        userOperationsClient.decidePayRequest(coworkingId, payRequestId, request);
    }

    public List<ServiceRequestQueueItemResponse> getServiceRequestQueue(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getServiceRequests(coworkingId);
    }

    public ServiceRequestWorkspaceResponse getServiceRequestWorkspace(Long coworkingId, Long serviceRequestId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_READ);
        return userOperationsClient.getServiceRequestWorkspace(coworkingId, serviceRequestId);
    }

    public ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        return userOperationsClient.addServiceRequestMessage(coworkingId, serviceRequestId, text, file);
    }

    public void decideServiceRequest(Long coworkingId, Long requestId, DecisionRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.USER_EDIT);
        userOperationsClient.decideServiceRequest(coworkingId, requestId, request);
    }

    public List<PlaceOperationalResponse> getOperationalPlaces(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.BOOKING_READ);
        return placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .filter(place -> floorId == null || place.getFloor().getId().equals(floorId))
                .map(place -> {
                    var bookings = userOperationsClient.getPlaceBookings(coworkingId, place.getId()).bookings();
                    return PlaceOperationalResponse.builder()
                            .placeId(place.getId())
                            .placeName(place.getName())
                            .placeTypeName(place.getPlaceType().getName())
                            .totalBookings(bookings.size())
                            .unfinishedBookings((int) bookings.stream()
                                    .filter(item -> Boolean.TRUE.equals(item.active()))
                                    .count())
                            .active(place.getActive())
                            .floorId(place.getFloor().getId())
                            .build();
                })
                .toList();
    }


    private OperationalImpactResponse enrichImpactResponse(OperationalImpactResponse response) {
        Map<Long, String> placeNames = placeRepository.findAllById(response.affectedBookings()
                .stream()
                .map(item -> item.placeId())
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(
                place -> place.getId(),
                place -> place.getName()
        ));
        return new OperationalImpactResponse(
                response.affectedBookingsCount(),
                response.affectedDates(),
                response.affectedBookings()
                        .stream()
                        .map(item -> new com.hse.adminservice.operations.bookingimpact.dto.AffectedBookingResponse(
                                item.bookingId(),
                                item.bookingNumber(),
                                item.membershipId(),
                                item.userId(),
                                item.userName(),
                                item.placeId(),
                                placeNames.getOrDefault(item.placeId(), item.placeName()),
                                item.date(),
                                item.bookingAmount(),
                                item.compensationAmount()
                        ))
                        .toList(),
                response.totalCompensationAmount(),
                response.impactHash()
        );
    }

    private MembershipProfileResponse enrichMembershipProfile(MembershipProfileResponse profile) {
        Map<Long, String> placeNames = placeRepository.findAllById(profile.activeBookings()
                .stream()
                .map(MembershipBookingResponse::placeId)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(
                place -> place.getId(),
                place -> place.getName()
        ));
        List<MembershipBookingResponse> bookings = profile.activeBookings()
                .stream()
                .map(booking -> new MembershipBookingResponse(
                        booking.bookingId(),
                        booking.bookingNumber(),
                        booking.placeId(),
                        placeNames.getOrDefault(booking.placeId(), "Место удалено"),
                        booking.date(),
                        booking.cost(),
                        booking.status()
                ))
                .toList();
        return new MembershipProfileResponse(
                profile.membershipId(),
                profile.userId(),
                profile.userName(),
                profile.userEmail(),
                profile.userDescription(),
                profile.status(),
                profile.createdAt(),
                profile.approvedAt(),
                profile.blockedAt(),
                profile.balanceMinorUnits(),
                bookings
        );
    }

}
