package com.hse.adminservice.testinfra;

import com.hse.adminservice.integration.user.port.UserOperationsClient;
import com.hse.adminservice.operations.booking.dto.PlaceBookingAdminResponse;
import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.AffectedBookingResponse;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.operations.common.DecisionRequest;
import com.hse.adminservice.operations.dashboard.OperationsDashboardResponse;
import com.hse.adminservice.operations.membership.dto.ManualBalanceAdjustmentRequest;
import com.hse.adminservice.operations.membership.dto.MembershipBookingResponse;
import com.hse.adminservice.operations.membership.dto.MembershipListItemResponse;
import com.hse.adminservice.operations.membership.dto.MembershipProfileResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestAttachmentResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestDetailResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestWorkspaceResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FakeUserOperationsClient implements UserOperationsClient {
    private final List<String> calls = new ArrayList<>();
    private RuntimeException usersFailure;
    private RuntimeException membershipProfileFailure;
    private RuntimeException dashboardFailure;
    private Long profileBookingPlaceId = 901L;

    public List<String> calls() {
        return calls;
    }

    public void clear() {
        calls.clear();
        usersFailure = null;
        membershipProfileFailure = null;
        dashboardFailure = null;
        profileBookingPlaceId = 901L;
    }

    public void failUsersWith(RuntimeException exception) {
        usersFailure = exception;
    }

    public void failMembershipProfileWith(RuntimeException exception) {
        membershipProfileFailure = exception;
    }

    public void failDashboardWith(RuntimeException exception) {
        dashboardFailure = exception;
    }

    public void setProfileBookingPlaceId(Long placeId) {
        profileBookingPlaceId = placeId;
    }

    @Override
    public PlaceBookingListResponse getPlaceBookings(Long coworkingId, Long placeId) {
        calls.add("getPlaceBookings:%d:%d".formatted(coworkingId, placeId));
        return PlaceBookingListResponse.builder()
                .coworkingId(coworkingId)
                .placeId(placeId)
                .source("fake-user-service")
                .message("Loaded from test fake")
                .bookings(List.of(
                        PlaceBookingAdminResponse.builder()
                                .bookingId(7101L)
                                .bookingNumber("B-7101")
                                .membershipId(8101L)
                                .userName("Resident One")
                                .date(LocalDate.now().plusDays(1))
                                .cost(1_500L)
                                .active(true)
                                .status("ACTIVE")
                                .build(),
                        PlaceBookingAdminResponse.builder()
                                .bookingId(7102L)
                                .bookingNumber("B-7102")
                                .membershipId(8102L)
                                .userName("Resident Two")
                                .date(LocalDate.now().minusDays(1))
                                .cost(1_500L)
                                .active(false)
                                .status("CANCELLED")
                                .build()
                ))
                .build();
    }

    @Override
    public List<CoworkingUserReadModelResponse> getUsers(Long coworkingId) {
        calls.add("getUsers:%d".formatted(coworkingId));
        if (usersFailure != null) {
            throw usersFailure;
        }
        return List.of(CoworkingUserReadModelResponse.builder()
                .userId(501L)
                .name("Resident One")
                .registeredAt(LocalDate.now().minusDays(30))
                .balance(25_000L)
                .totalBookings(4)
                .unfinishedBookings(1)
                .build());
    }

    @Override
    public List<MembershipListItemResponse> getMembershipList(Long coworkingId, String search) {
        calls.add("getMembershipList:%d:%s".formatted(coworkingId, search));
        return List.of(new MembershipListItemResponse(
                601L,
                501L,
                "Resident One",
                "resident.one@example.test",
                "ACTIVE",
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(19),
                null,
                25_000L,
                1
        ));
    }

    @Override
    public MembershipProfileResponse getMembershipProfile(Long coworkingId, Long membershipId) {
        calls.add("getMembershipProfile:%d:%d".formatted(coworkingId, membershipId));
        if (membershipProfileFailure != null) {
            throw membershipProfileFailure;
        }
        return membershipProfile(membershipId, 25_000L, List.of(new MembershipBookingResponse(
                701L,
                "B-701",
                profileBookingPlaceId,
                "Name from user service should be replaced",
                LocalDate.now().plusDays(1),
                1_500L,
                "ACTIVE"
        )));
    }

    @Override
    public MembershipProfileResponse adjustMembershipBalance(
            Long coworkingId,
            Long membershipId,
            ManualBalanceAdjustmentRequest request
    ) {
        calls.add("adjustMembershipBalance:%d:%d:%d:%s".formatted(
                coworkingId,
                membershipId,
                request.amountMinorUnits(),
                request.comment()
        ));
        return membershipProfile(membershipId, 25_000L + request.amountMinorUnits(), List.of());
    }



    @Override
    public OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId) {
        calls.add("previewMembershipBlock:%d:%d".formatted(coworkingId, membershipId));
        return membershipBlockImpact(membershipId, "membership-block-preview-hash");
    }

    @Override
    public OperationalImpactResponse blockMembership(Long coworkingId, Long membershipId, ImpactCommitRequest request) {
        calls.add("blockMembership:%d:%d:%s".formatted(coworkingId, membershipId, request.getImpactHash()));
        return membershipBlockImpact(membershipId, request.getImpactHash());
    }

    @Override
    public OperationsDashboardResponse getOperationsDashboard(Long coworkingId) {
        calls.add("getOperationsDashboard:%d".formatted(coworkingId));
        if (dashboardFailure != null) {
            throw dashboardFailure;
        }
        return new OperationsDashboardResponse(
                new OperationsDashboardResponse.Queues(2, 1, 3),
                new OperationsDashboardResponse.Memberships(10, 7, 2, 1),
                new OperationsDashboardResponse.Finance(100_000L, 45_000L),
                new OperationsDashboardResponse.Occupancy(67)
        );
    }

    @Override
    public List<MembershipQueueItemResponse> getMemberships(Long coworkingId) {
        calls.add("getMemberships:%d".formatted(coworkingId));
        return List.of(MembershipQueueItemResponse.builder()
                .membershipId(601L)
                .userId(501L)
                .userName("Resident One")
                .coworkingName("Volga Hub")
                .status("PENDING")
                .createdAt(LocalDate.now().minusDays(2))
                .build());
    }

    @Override
    public void decideMembership(Long coworkingId, Long membershipId, DecisionRequest request) {
        calls.add("decideMembership:%d:%d:%s:%s".formatted(coworkingId, membershipId, request.decision(), request.comment()));
    }

    @Override
    public List<PayRequestQueueItemResponse> getPayRequests(Long coworkingId) {
        calls.add("getPayRequests:%d".formatted(coworkingId));
        return List.of(PayRequestQueueItemResponse.builder()
                .payRequestId(801L)
                .membershipId(601L)
                .userId(501L)
                .userName("Resident One")
                .amount(10_000L)
                .status("PENDING")
                .userComment("Top up")
                .adminComment(null)
                .createdAt(LocalDate.now().minusDays(1))
                .build());
    }

    @Override
    public void decidePayRequest(Long coworkingId, Long payRequestId, DecisionRequest request) {
        calls.add("decidePayRequest:%d:%d:%s:%s".formatted(coworkingId, payRequestId, request.decision(), request.comment()));
    }

    @Override
    public List<ServiceRequestQueueItemResponse> getServiceRequests(Long coworkingId) {
        calls.add("getServiceRequests:%d".formatted(coworkingId));
        return List.of(ServiceRequestQueueItemResponse.builder()
                .serviceRequestId(901L)
                .membershipId(601L)
                .userId(501L)
                .userName("Resident One")
                .typeName("Cleaning")
                .name("Clean meeting room")
                .cost(2_000L)
                .status("OPEN")
                .createdAt(LocalDate.now())
                .build());
    }

    @Override
    public ServiceRequestWorkspaceResponse getServiceRequestWorkspace(Long coworkingId, Long serviceRequestId) {
        calls.add("getServiceRequestWorkspace:%d:%d".formatted(coworkingId, serviceRequestId));
        return new ServiceRequestWorkspaceResponse(
                ServiceRequestDetailResponse.builder()
                        .serviceRequestId(serviceRequestId)
                        .membershipId(601L)
                        .userId(501L)
                        .userName("Resident One")
                        .userEmail("resident.one@example.test")
                        .typeName("Cleaning")
                        .name("Clean meeting room")
                        .cost(2_000L)
                        .balanceMinorUnits(25_000L)
                        .status("OPEN")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .updatedAt(LocalDateTime.now().minusHours(1))
                        .resolvedAt(null)
                        .build(),
                List.of(ServiceRequestMessageResponse.builder()
                        .id(1001L)
                        .authorType("USER")
                        .authorName("Resident One")
                        .text("Please clean the room")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .attachments(List.of())
                        .build()),
                List.of("IN_PROGRESS", "RESOLVE", "REJECT")
        );
    }

    @Override
    public ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    ) {
        calls.add("addServiceRequestMessage:%d:%d:%s:%s".formatted(
                coworkingId,
                serviceRequestId,
                text,
                file == null ? "no-file" : file.getOriginalFilename()
        ));
        return ServiceRequestMessageResponse.builder()
                .id(1002L)
                .authorType("ADMIN")
                .authorName("Admin")
                .text(text)
                .createdAt(LocalDateTime.now())
                .attachments(file == null ? List.of() : List.of(new ServiceRequestAttachmentResponse(
                        2001L,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getSize(),
                        "https://files.test/" + file.getOriginalFilename()
                )))
                .build();
    }

    @Override
    public void decideServiceRequest(Long coworkingId, Long serviceRequestId, DecisionRequest request) {
        calls.add("decideServiceRequest:%d:%d:%s:%s".formatted(coworkingId, serviceRequestId, request.decision(), request.comment()));
    }



    private OperationalImpactResponse membershipBlockImpact(Long membershipId, String impactHash) {
        return new OperationalImpactResponse(
                1,
                List.of(LocalDate.now().plusDays(1).toString()),
                List.of(new AffectedBookingResponse(
                        7101L,
                        "B-7101",
                        membershipId,
                        501L,
                        "Resident One",
                        profileBookingPlaceId,
                        "Name from user service should be replaced",
                        LocalDate.now().plusDays(1),
                        java.math.BigDecimal.valueOf(1_500L),
                        java.math.BigDecimal.valueOf(1_500L)
                )),
                1_500L,
                impactHash
        );
    }

    private MembershipProfileResponse membershipProfile(
            Long membershipId,
            Long balanceMinorUnits,
            List<MembershipBookingResponse> bookings
    ) {
        return new MembershipProfileResponse(
                membershipId,
                501L,
                "Resident One",
                "resident.one@example.test",
                "Regular resident",
                "ACTIVE",
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(19),
                null,
                balanceMinorUnits,
                bookings
        );
    }
}
