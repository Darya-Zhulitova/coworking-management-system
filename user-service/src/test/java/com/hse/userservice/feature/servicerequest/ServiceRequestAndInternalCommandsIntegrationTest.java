package com.hse.userservice.feature.servicerequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.userservice.common.files.FileStorageService;
import com.hse.userservice.common.files.StoredFileResponse;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.servicerequest.domain.Message;
import com.hse.userservice.feature.servicerequest.domain.MessageAuthorType;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequest;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;
import com.hse.userservice.feature.servicerequest.repository.MessageRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestAttachmentRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestRepository;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import com.hse.userservice.support.IntegrationTestSupport;
import com.hse.userservice.support.config.TestAdminServiceClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceRequestAndInternalCommandsIntegrationTest extends IntegrationTestSupport {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final Long COWORKING_ID = 1L;
    private static final Long FREE_TYPE_ID = 1L;
    private static final Long PAID_TYPE_ID = 2L;
    private static final Long PLACE_ID = 1L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAdminServiceClientConfig.FakeAdminServiceClient adminServiceClient;
    @Autowired private ServiceRequestRepository serviceRequestRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ServiceRequestAttachmentRepository attachmentRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private UnitsService unitsService;

    @MockitoBean private FileStorageService fileStorageService;

    @BeforeEach
    void resetExternalStubs() {
        adminServiceClient.reset();
        adminServiceClient.putServiceRequestTypes(COWORKING_ID, List.of(
                new ServiceRequestTypeInfo(FREE_TYPE_ID, "Water delivery", 0L, 1, true),
                new ServiceRequestTypeInfo(PAID_TYPE_ID, "Paid cleaning", 750L, 2, true),
                new ServiceRequestTypeInfo(99L, "Disabled type", 100L, 3, false)
        ));
        adminServiceClient.putBookingContext(bookingContext());
        adminServiceClient.putPlaceSummaries(COWORKING_ID, List.of(new PlaceSummary(PLACE_ID, "Desk 1", "Floor 1", "Desk", null, null)));
        when(fileStorageService.uploadServiceRequestAttachment(any(Long.class), any(Long.class), any()))
                .thenAnswer(invocation -> new StoredFileResponse(
                        "service-requests/%d/test.txt".formatted(invocation.getArgument(0, Long.class)),
                        "https://files.test/request.txt"
                ));
        when(fileStorageService.presignedUrl(any(String.class)))
                .thenAnswer(invocation -> "https://files.test/" + invocation.getArgument(0, String.class));
    }

    @Test
    void residentCreatesServiceRequestAndSystemMessageIsSeeded() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("service-create"));

        mockMvc.perform(get("/api/memberships/{membershipId}/service-requests/types", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(FREE_TYPE_ID))
                .andExpect(jsonPath("$[0].name").value("Water delivery"));

        Long serviceRequestId = createServiceRequest(authMembership, FREE_TYPE_ID, "  Replace water bottle  ");

        ServiceRequest saved = serviceRequestRepository.findById(serviceRequestId).orElseThrow();
        assertThat(saved.getMembershipId()).isEqualTo(authMembership.membershipId());
        assertThat(saved.getName()).isEqualTo("Replace water bottle");
        assertThat(saved.getTypeId()).isEqualTo(FREE_TYPE_ID);
        assertThat(saved.getTypeName()).isEqualTo("Water delivery");
        assertThat(saved.getCost()).isZero();
        assertThat(saved.getStatus()).isEqualTo(ServiceRequestStatus.NEW);

        List<Message> messages = messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(serviceRequestId);
        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.getAuthorType()).isEqualTo(MessageAuthorType.SYSTEM);
            assertThat(message.getText()).contains("Сервисная заявка создана пользователем");
        });

        mockMvc.perform(get("/api/memberships/{membershipId}/service-requests", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(serviceRequestId))
                .andExpect(jsonPath("$[0].typeName").value("Water delivery"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void userAndAdminMessagesSupportAttachmentsAndAreVisibleInWorkspace() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("service-messages"));
        Long serviceRequestId = createServiceRequest(authMembership, FREE_TYPE_ID, "Printer paper");
        MockMultipartFile attachment = new MockMultipartFile("file", "photo.txt", "text/plain", "paper is missing".getBytes());

        mockMvc.perform(multipart("/api/memberships/{membershipId}/service-requests/{requestId}/messages", authMembership.membershipId(), serviceRequestId)
                        .file(attachment)
                        .param("text", "  Please see attachment  ")
                        .header("Authorization", bearer(authMembership.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorType").value("USER"))
                .andExpect(jsonPath("$.text").value("Please see attachment"))
                .andExpect(jsonPath("$.attachments[0].fileName").value("photo.txt"))
                .andExpect(jsonPath("$.attachments[0].contentType").value("text/plain"));

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/messages", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "Admin accepted the request"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorType").value("ADMIN"))
                .andExpect(jsonPath("$.authorName").value("Администратор"));

        assertThat(attachmentRepository.findAllByMessageIdIn(messageIds(serviceRequestId))).hasSize(1);

        mockMvc.perform(get("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/workspace", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.serviceRequestId").value(serviceRequestId))
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.availableActions[0]").value("REPLY"));
    }

    @Test
    void internalApiChangesServiceRequestStatusesAndChargesPaidRequestOnResolve() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("service-paid"));
        topUp(authMembership.membershipId(), 2_000L);
        Long serviceRequestId = createServiceRequest(authMembership, PAID_TYPE_ID, "Clean meeting room");

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "IN_PROGRESS", "comment", "Specialist assigned"))))
                .andExpect(status().isNoContent());

        assertThat(serviceRequestRepository.findById(serviceRequestId).orElseThrow().getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "RESOLVE", "comment", "Cleaning completed"))))
                .andExpect(status().isNoContent());

        ServiceRequest resolved = serviceRequestRepository.findById(serviceRequestId).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(ServiceRequestStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(1_250L);
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .anySatisfy(entry -> {
                    assertThat(entry.getType()).isEqualTo(LedgerEntryType.SERVICE_REQUEST_CHARGE);
                    assertThat(entry.getAmount()).isEqualTo(-750L);
                    assertThat(entry.getReferenceId()).isEqualTo(serviceRequestId);
                });
    }

    @Test
    void paidServiceRequestCannotBeResolvedWhenBalanceIsInsufficient() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("service-insufficient"));
        Long serviceRequestId = createServiceRequest(authMembership, PAID_TYPE_ID, "Paid cleaning without funds");
        long ledgerCountBefore = ledgerEntryRepository.count();

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "RESOLVE", "comment", "Cannot close for free"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]").value("Сервисную заявку нельзя закрыть: на балансе пользователя недостаточно средств."));

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId).orElseThrow();
        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.NEW);
        assertThat(request.getResolvedAt()).isNull();
        assertThat(ledgerEntryRepository.count()).isEqualTo(ledgerCountBefore);
    }

    @Test
    void rejectedServiceRequestBecomesFinalAndRejectsFurtherMessages() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("service-reject"));
        Long serviceRequestId = createServiceRequest(authMembership, FREE_TYPE_ID, "Impossible request");

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision", COWORKING_ID, serviceRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "REJECT", "comment", "Not supported"))))
                .andExpect(status().isNoContent());

        assertThat(serviceRequestRepository.findById(serviceRequestId).orElseThrow().getStatus()).isEqualTo(ServiceRequestStatus.REJECTED);

        mockMvc.perform(multipart("/api/memberships/{membershipId}/service-requests/{requestId}/messages", authMembership.membershipId(), serviceRequestId)
                        .param("text", "Why rejected?")
                        .header("Authorization", bearer(authMembership.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]").value("Нельзя добавить сообщение в закрытую сервисную заявку."));
    }

    @Test
    void internalApiBlocksActiveMembership() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("membership-block"));

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/memberships/{membershipId}/block", COWORKING_ID, authMembership.membershipId())
                        .header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isNoContent());

        Membership blocked = membershipRepository.findById(authMembership.membershipId()).orElseThrow();
        assertThat(blocked.getStatus()).isEqualTo(MembershipStatus.BLOCKED);
        assertThat(blocked.getBlockedAt()).isNotNull();
    }

    @Test
    void internalDayClosingCommitCancelsAffectedBookingsAndCreatesCompensationLedgerEntry() throws Exception {
        LocalDate date = availableDate(30);
        AuthMembership authMembership = registerAndJoin(uniqueEmail("day-closing"));
        topUp(authMembership.membershipId(), 5_000L);
        Long bookingId = createBooking(authMembership, PLACE_ID, date);

        String preview = mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/booking-impact/day-closing/preview", COWORKING_ID)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", date.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedBookingsCount").value(1))
                .andExpect(jsonPath("$.affectedBookings[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$.totalCompensationAmount").value(1_000L))
                .andReturn().getResponse().getContentAsString();
        String impactHash = objectMapper.readTree(preview).get("impactHash").asText();

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/booking-impact/day-closing/commit", COWORKING_ID)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("date", date.toString(), "impactHash", impactHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedBookingsCount").value(1));

        Booking cancelled = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelled.getActive()).isFalse();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELED_ADMIN);
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(5_000L);
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .anySatisfy(entry -> {
                    assertThat(entry.getType()).isEqualTo(LedgerEntryType.BOOKING_ADMIN_CANCELLATION_COMPENSATION);
                    assertThat(entry.getAmount()).isEqualTo(1_000L);
                    assertThat(entry.getReferenceId()).isEqualTo(bookingId);
                });
    }

    @Test
    void internalPlaceClosingAndDeactivationPreviewReturnAffectedBookings() throws Exception {
        LocalDate date = availableDate(45);
        AuthMembership authMembership = registerAndJoin(uniqueEmail("place-impact"));
        topUp(authMembership.membershipId(), 5_000L);
        Long bookingId = createBooking(authMembership, PLACE_ID, date);

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/booking-impact/place-closing/preview", COWORKING_ID)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("placeId", PLACE_ID, "date", date.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedBookingsCount").value(1))
                .andExpect(jsonPath("$.affectedBookings[0].bookingId").value(bookingId));

        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/booking-impact/place-deactivation/preview", COWORKING_ID)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("placeId", PLACE_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedBookingsCount").value(1))
                .andExpect(jsonPath("$.affectedBookings[0].bookingId").value(bookingId));
    }

    private List<Long> messageIds(Long serviceRequestId) {
        return messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(serviceRequestId).stream().map(Message::getId).toList();
    }

    private Long createServiceRequest(AuthMembership authMembership, Long typeId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/memberships/{membershipId}/service-requests", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("typeId", typeId, "name", name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeId").value(typeId))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createBooking(AuthMembership authMembership, Long placeId, LocalDate date) throws Exception {
        String response = mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("items", List.of(Map.of("placeId", placeId, "date", date.toString()))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("bookings").get(0).get("id").asLong();
    }

    private AuthMembership registerAndJoin(String email) throws Exception {
        String token = registerAndGetToken(email);
        String response = mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn().getResponse().getContentAsString();
        return new AuthMembership(token, objectMapper.readTree(response).get("membershipId").asLong());
    }

    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "secret123", "name", "Service Resident", "description", "service request flow"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private void topUp(Long membershipId, Long amountMinorUnits) throws Exception {
        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/memberships/{membershipId}/balance-adjustments", COWORKING_ID, membershipId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amountMinorUnits", amountMinorUnits, "comment", "Service request test top-up"))))
                .andExpect(status().isOk());
    }

    private LocalDate availableDate(int offsetDays) {
        return LocalDate.now().plusDays(offsetDays);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private BookingContext bookingContext() {
        return new BookingContext(
                COWORKING_ID,
                1L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                127,
                "Test Coworking",
                true,
                List.of(new BookingContext.Floor(1L, "Floor 1", 1, null, null, true)),
                List.of(new BookingContext.Tariff(1L, "Daily desk", 1_000L, 24, 100, new BigDecimal("1.0000"), new BigDecimal("1.0000"), new BigDecimal("1.0000"), 1, true)),
                List.of(new BookingContext.PlaceType(1L, "Desk", 1L, true)),
                List.of(new BookingContext.Place(PLACE_ID, "Desk 1", 1L, 1L, new BigDecimal("0.5000"), new BigDecimal("0.5000"), null, null, null, List.of("socket", "wifi"), true)),
                List.of(),
                List.of()
        );
    }

    private record AuthMembership(String token, Long membershipId) {
    }
}
