package com.hse.userservice.concurrency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.repository.PayRequestRepository;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.support.IntegrationTestSupport;
import com.hse.userservice.support.config.TestAdminServiceClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingAndBalanceConcurrencyIntegrationTest extends IntegrationTestSupport {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final Long COWORKING_ID = 1L;
    private static final Long DESK_PLACE_ID = 1L;
    private static final Long ROOM_PLACE_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAdminServiceClientConfig.FakeAdminServiceClient adminServiceClient;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PayRequestRepository payRequestRepository;

    @Autowired
    private UnitsService unitsService;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        adminServiceClient.reset();
        adminServiceClient.putBookingContext(bookingContext());
        adminServiceClient.putPlaceSummaries(COWORKING_ID, List.of(
                new PlaceSummary(DESK_PLACE_ID, "Desk 1", "Floor 1", "Desk", null, null),
                new PlaceSummary(ROOM_PLACE_ID, "Room 1", "Floor 1", "Meeting room", null, null)
        ));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentCheckoutAllowsOnlyOneActiveBookingForSamePlaceAndDate() throws Exception {
        LocalDate date = availableDate(365);
        AuthMembership firstResident = registerJoinAndTopUp(uniqueEmail("concurrent-place-first"), 5_000L);
        AuthMembership secondResident = registerJoinAndTopUp(uniqueEmail("concurrent-place-second"), 5_000L);

        List<Integer> statuses = runConcurrently(
                () -> createBookingStatus(firstResident, DESK_PLACE_ID, date),
                () -> createBookingStatus(secondResident, DESK_PLACE_ID, date)
        );

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(List.of(DESK_PLACE_ID), List.of(date)))
                .hasSize(1);
        assertThat(bookingChargeEntries(firstResident.membershipId()).size()
                + bookingChargeEntries(secondResident.membershipId()).size())
                .isEqualTo(1);
        assertThat(List.of(
                unitsService.getBalanceMinorUnits(firstResident.membershipId()),
                unitsService.getBalanceMinorUnits(secondResident.membershipId())
        )).containsExactlyInAnyOrder(4_000L, 5_000L);
    }

    @Test
    void concurrentCheckoutForSameMembershipDoesNotOverspendBalance() throws Exception {
        LocalDate firstDate = availableDate(380);
        LocalDate secondDate = firstDate.plusDays(1);
        AuthMembership resident = registerJoinAndTopUp(uniqueEmail("concurrent-balance"), 1_000L);

        List<Integer> statuses = runConcurrently(
                () -> createBookingStatus(resident, DESK_PLACE_ID, firstDate),
                () -> createBookingStatus(resident, DESK_PLACE_ID, secondDate)
        );

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(bookingRepository.findAllByMembershipIdOrderByDateDesc(resident.membershipId()))
                .filteredOn(Booking::getActive)
                .hasSize(1);
        assertThat(bookingChargeEntries(resident.membershipId())).hasSize(1);
        assertThat(unitsService.getBalanceMinorUnits(resident.membershipId())).isZero();
    }

    @Test
    void concurrentCancellationCreatesOnlyOneRefund() throws Exception {
        LocalDate date = availableDate(395);
        AuthMembership resident = registerJoinAndTopUp(uniqueEmail("concurrent-cancel"), 5_000L);
        Long bookingId = createBooking(resident, DESK_PLACE_ID, date);

        List<Integer> statuses = runConcurrently(
                () -> cancelBookingStatus(resident, bookingId),
                () -> cancelBookingStatus(resident, bookingId)
        );

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED_USER);
        assertThat(booking.getActive()).isFalse();
        assertThat(refundEntries(resident.membershipId(), bookingId)).hasSize(1);
        assertThat(unitsService.getBalanceMinorUnits(resident.membershipId())).isEqualTo(5_000L);
    }

    @Test
    void concurrentPayRequestApprovalCreatesOnlyOneLedgerEntry() throws Exception {
        AuthMembership resident = registerAndJoin(uniqueEmail("concurrent-pay-request"));
        Long payRequestId = createPayRequest(resident, 3_000L);

        List<Integer> statuses = runConcurrently(
                () -> approvePayRequestStatus(payRequestId),
                () -> approvePayRequestStatus(payRequestId)
        );

        assertThat(statuses).containsExactlyInAnyOrder(204, 409);
        PayRequest payRequest = payRequestRepository.findById(payRequestId).orElseThrow();
        assertThat(payRequest.getStatus()).isEqualTo(PayRequestStatus.APPROVED);
        assertThat(unitsService.getBalanceMinorUnits(resident.membershipId())).isEqualTo(3_000L);
        assertThat(payRequestLedgerEntries(resident.membershipId(), payRequestId)).hasSize(1);
    }

    private List<Integer> runConcurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Integer> firstResult = executor.submit(awaitStart(first, ready, start));
        Future<Integer> secondResult = executor.submit(awaitStart(second, ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
    }

    private Callable<Integer> awaitStart(Callable<Integer> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return action.call();
        };
    }

    private int createBookingStatus(AuthMembership authMembership, Long placeId, LocalDate date) throws Exception {
        return mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(item(placeId, date)))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private Long createBooking(AuthMembership authMembership, Long placeId, LocalDate date) throws Exception {
        String response = mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(item(placeId, date)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("bookings").get(0).get("id").asLong();
    }

    private int cancelBookingStatus(AuthMembership authMembership, Long bookingId) throws Exception {
        return mockMvc.perform(post("/api/memberships/{membershipId}/bookings/{bookingId}/cancel", authMembership.membershipId(), bookingId)
                        .header("Authorization", bearer(authMembership.token())))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int approvePayRequestStatus(Long payRequestId) throws Exception {
        return mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/pay-requests/{payRequestId}/decision", COWORKING_ID, payRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVE",
                                "comment", "Concurrent payment confirmation"
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private Long createPayRequest(AuthMembership authMembership, Long amountMinorUnits) throws Exception {
        String response = mockMvc.perform(post("/api/memberships/{membershipId}/pay-requests", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", amountMinorUnits,
                                "userComment", "Concurrent top up"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private AuthMembership registerJoinAndTopUp(String email, Long amountMinorUnits) throws Exception {
        AuthMembership authMembership = registerAndJoin(email);
        topUp(authMembership.membershipId(), amountMinorUnits);
        return authMembership;
    }

    private AuthMembership registerAndJoin(String email) throws Exception {
        String token = registerAndGetToken(email);
        String response = mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new AuthMembership(token, objectMapper.readTree(response).get("membershipId").asLong());
    }

    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "name", "Concurrency Resident",
                                "description", "concurrency test"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private void topUp(Long membershipId, Long amountMinorUnits) throws Exception {
        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/memberships/{membershipId}/balance-adjustments", COWORKING_ID, membershipId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountMinorUnits", amountMinorUnits,
                                "comment", "Concurrency test top-up"
                        ))))
                .andExpect(status().isOk());
    }

    private List<LedgerEntry> bookingChargeEntries(Long membershipId) {
        return ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membershipId).stream()
                .filter(entry -> entry.getType() == LedgerEntryType.BOOKING_CHARGE)
                .toList();
    }

    private List<LedgerEntry> refundEntries(Long membershipId, Long bookingId) {
        return ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membershipId).stream()
                .filter(entry -> entry.getType() == LedgerEntryType.BOOKING_USER_CANCELLATION_REFUND)
                .filter(entry -> entry.getReferenceId().equals(bookingId))
                .toList();
    }

    private List<LedgerEntry> payRequestLedgerEntries(Long membershipId, Long payRequestId) {
        return ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membershipId).stream()
                .filter(entry -> entry.getType() == LedgerEntryType.BALANCE_TOP_UP)
                .filter(entry -> entry.getReferenceId().equals(payRequestId))
                .toList();
    }

    private String cartJson(List<Map<String, Object>> items) throws Exception {
        return objectMapper.writeValueAsString(Map.of("items", items));
    }

    private Map<String, Object> item(Long placeId, LocalDate date) {
        return Map.of("placeId", placeId, "date", date.toString());
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
                List.of(
                        new BookingContext.Tariff(
                                1L,
                                "Daily desk",
                                1_000L,
                                24,
                                100,
                                new BigDecimal("1.0000"),
                                new BigDecimal("1.0000"),
                                new BigDecimal("1.0000"),
                                1,
                                true
                        ),
                        new BookingContext.Tariff(
                                2L,
                                "Daily room",
                                2_500L,
                                24,
                                100,
                                new BigDecimal("1.0000"),
                                new BigDecimal("1.0000"),
                                new BigDecimal("1.0000"),
                                1,
                                true
                        )
                ),
                List.of(
                        new BookingContext.PlaceType(1L, "Desk", 1L, true),
                        new BookingContext.PlaceType(2L, "Meeting room", 2L, true)
                ),
                List.of(
                        new BookingContext.Place(
                                DESK_PLACE_ID,
                                "Desk 1",
                                1L,
                                1L,
                                new BigDecimal("0.5000"),
                                new BigDecimal("0.5000"),
                                null,
                                null,
                                null,
                                List.of("socket", "wifi"),
                                true
                        ),
                        new BookingContext.Place(
                                ROOM_PLACE_ID,
                                "Room 1",
                                1L,
                                2L,
                                new BigDecimal("0.7000"),
                                new BigDecimal("0.5000"),
                                null,
                                null,
                                null,
                                List.of("screen", "whiteboard"),
                                true
                        )
                ),
                List.of(),
                List.of()
        );
    }

    private record AuthMembership(String token, Long membershipId) {
    }
}
