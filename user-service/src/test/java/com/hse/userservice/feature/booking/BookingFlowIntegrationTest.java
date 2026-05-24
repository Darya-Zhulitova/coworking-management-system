package com.hse.userservice.feature.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.support.IntegrationTestSupport;
import com.hse.userservice.support.config.TestAdminServiceClientConfig;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowIntegrationTest extends IntegrationTestSupport {
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
    private UnitsService unitsService;

    @BeforeEach
    void resetAdminContext() {
        adminServiceClient.reset();
        adminServiceClient.putBookingContext(bookingContext(List.of(), List.of()));
        adminServiceClient.putPlaceSummaries(COWORKING_ID, List.of(
                new PlaceSummary(DESK_PLACE_ID, "Desk 1", "Floor 1", "Desk", null, null),
                new PlaceSummary(ROOM_PLACE_ID, "Room 1", "Floor 1", "Meeting room", null, null)
        ));
    }

    @Test
    void bookingInitReturnsAvailablePlacesWithPricesAndCurrentBalance() throws Exception {
        LocalDate date = availableDate(10);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("booking-init"), 5_000L);

        mockMvc.perform(get("/api/memberships/{membershipId}/booking/init", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coworkingName").value("Test Coworking"))
                .andExpect(jsonPath("$.membershipId").value(authMembership.membershipId()))
                .andExpect(jsonPath("$.membershipStatus").value("active"))
                .andExpect(jsonPath("$.balanceMinorUnits").value(5_000L))
                .andExpect(jsonPath("$.previewDate").value(date.toString()))
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].id").value(DESK_PLACE_ID))
                .andExpect(jsonPath("$.places[0].pricePerDay").value(1_000L))
                .andExpect(jsonPath("$.places[0].available").value(true))
                .andExpect(jsonPath("$.places[1].id").value(ROOM_PLACE_ID))
                .andExpect(jsonPath("$.places[1].pricePerDay").value(2_500L));
    }

    @Test
    void placeAvailabilityReflectsExistingBookingsAndAdminClosings() throws Exception {
        LocalDate firstDate = availableDate(20);
        LocalDate secondDate = firstDate.plusDays(1);
        LocalDate closedDate = firstDate.plusDays(2);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("availability"), 10_000L);
        createBooking(authMembership, DESK_PLACE_ID, firstDate);
        adminServiceClient.putBookingContext(bookingContext(
                List.of(),
                List.of(new BookingContext.PlaceClosing(100L, DESK_PLACE_ID, closedDate, "Maintenance", true))
        ));

        mockMvc.perform(get("/api/memberships/{membershipId}/places/{placeId}/availability", authMembership.membershipId(), DESK_PLACE_ID)
                        .header("Authorization", bearer(authMembership.token()))
                        .param("from", firstDate.toString())
                        .param("to", closedDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value(firstDate.toString()))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[1].date").value(secondDate.toString()))
                .andExpect(jsonPath("$[1].available").value(true))
                .andExpect(jsonPath("$[2].date").value(closedDate.toString()))
                .andExpect(jsonPath("$[2].available").value(false));
    }

    @Test
    void cartCalculationUsesTariffsAvailabilityAndBalance() throws Exception {
        LocalDate date = availableDate(30);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("cart-calc"), 3_000L);

        mockMvc.perform(post("/api/memberships/{membershipId}/bookings/cart/calculate", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(
                                item(DESK_PLACE_ID, date),
                                item(ROOM_PLACE_ID, date)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].finalPrice").value(1_000L))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[1].finalPrice").value(2_500L))
                .andExpect(jsonPath("$.summary.totalFinalPrice").value(3_500L))
                .andExpect(jsonPath("$.summary.hasEnoughBalance").value(false))
                .andExpect(jsonPath("$.summary.balanceAfterMinorUnits").value(-500L))
                .andExpect(jsonPath("$.summary.canCheckout").value(false));
    }

    @Test
    void createBookingCreatesBookingAndBookingChargeLedgerEntryAtomically() throws Exception {
        LocalDate date = availableDate(40);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("booking-create"), 5_000L);

        Long bookingId = createBooking(authMembership, DESK_PLACE_ID, date);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getMembershipId()).isEqualTo(authMembership.membershipId());
        assertThat(booking.getPlaceId()).isEqualTo(DESK_PLACE_ID);
        assertThat(booking.getDate()).isEqualTo(date);
        assertThat(booking.getCost()).isEqualTo(1_000L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTUAL);
        assertThat(booking.getActive()).isTrue();

        List<LedgerEntry> ledger = ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId());
        assertThat(ledger).anySatisfy(entry -> {
            assertThat(entry.getType()).isEqualTo(LedgerEntryType.BOOKING_CHARGE);
            assertThat(entry.getAmount()).isEqualTo(-1_000L);
            assertThat(entry.getReferenceId()).isEqualTo(bookingId);
        });
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(4_000L);
    }

    @Test
    void insufficientBalancePreventsBookingAndDoesNotCreatePartialLedgerEntry() throws Exception {
        LocalDate date = availableDate(50);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("booking-insufficient"), 500L);
        long bookingCountBefore = bookingRepository.count();
        int ledgerCountBefore = ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()).size();

        mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(item(DESK_PLACE_ID, date)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]").value("На балансе недостаточно средств для бронирования."));

        assertThat(bookingRepository.count()).isEqualTo(bookingCountBefore);
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .hasSize(ledgerCountBefore);
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(500L);
    }

    @Test
    void occupiedPlaceCannotBeBookedTwiceAndSecondAttemptDoesNotCreateCharge() throws Exception {
        LocalDate date = availableDate(60);
        AuthMembership firstResident = registerJoinAndTopUp(uniqueEmail("booking-owner"), 5_000L);
        AuthMembership secondResident = registerJoinAndTopUp(uniqueEmail("booking-conflict"), 5_000L);
        createBooking(firstResident, DESK_PLACE_ID, date);
        long bookingCountBeforeSecondAttempt = bookingRepository.count();
        int secondResidentLedgerCountBefore = ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(secondResident.membershipId()).size();

        mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", secondResident.membershipId())
                        .header("Authorization", bearer(secondResident.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(item(DESK_PLACE_ID, date)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]").value("Одно или несколько мест уже недоступны для бронирования."));

        assertThat(bookingRepository.count()).isEqualTo(bookingCountBeforeSecondAttempt);
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(secondResident.membershipId()))
                .hasSize(secondResidentLedgerCountBefore);
        assertThat(unitsService.getBalanceMinorUnits(secondResident.membershipId())).isEqualTo(5_000L);
    }

    @Test
    void duplicateCartItemsAreNormalizedBeforeCheckout() throws Exception {
        LocalDate date = availableDate(70);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("booking-duplicates"), 5_000L);

        String response = mockMvc.perform(post("/api/memberships/{membershipId}/bookings/create-from-cart", authMembership.membershipId())
                        .header("Authorization", bearer(authMembership.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartJson(List.of(
                                item(DESK_PLACE_ID, date),
                                item(DESK_PLACE_ID, date)
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalChargedMinorUnits").value(1_000L))
                .andExpect(jsonPath("$.balanceAfterMinorUnits").value(4_000L))
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long bookingId = objectMapper.readTree(response).get("bookings").get(0).get("id").asLong();
        assertThat(bookingRepository.findById(bookingId)).isPresent();
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId())
                .stream()
                .filter(entry -> entry.getType() == LedgerEntryType.BOOKING_CHARGE))
                .hasSize(1);
    }

    @Test
    void userCancellationDeactivatesBookingAndCreatesRefundLedgerEntry() throws Exception {
        LocalDate date = availableDate(80);
        AuthMembership authMembership = registerJoinAndTopUp(uniqueEmail("booking-cancel"), 5_000L);
        Long bookingId = createBooking(authMembership, DESK_PLACE_ID, date);

        mockMvc.perform(post("/api/memberships/{membershipId}/bookings/{bookingId}/cancel", authMembership.membershipId(), bookingId)
                        .header("Authorization", bearer(authMembership.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.refundMinorUnits").value(1_000L))
                .andExpect(jsonPath("$.balanceAfterMinorUnits").value(5_000L))
                .andExpect(jsonPath("$.booking.active").value(false))
                .andExpect(jsonPath("$.booking.status").value("CANCELED_USER"));

        Booking cancelled = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelled.getActive()).isFalse();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELED_USER);
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(5_000L);
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .anySatisfy(entry -> {
                    assertThat(entry.getType()).isEqualTo(LedgerEntryType.BOOKING_USER_CANCELLATION_REFUND);
                    assertThat(entry.getAmount()).isEqualTo(1_000L);
                    assertThat(entry.getReferenceId()).isEqualTo(bookingId);
                });
    }

    @Test
    void cancelledBookingFreesSlotForAnotherResident() throws Exception {
        LocalDate date = availableDate(90);
        AuthMembership firstResident = registerJoinAndTopUp(uniqueEmail("booking-free-first"), 5_000L);
        AuthMembership secondResident = registerJoinAndTopUp(uniqueEmail("booking-free-second"), 5_000L);
        Long firstBookingId = createBooking(firstResident, DESK_PLACE_ID, date);

        mockMvc.perform(post("/api/memberships/{membershipId}/bookings/{bookingId}/cancel", firstResident.membershipId(), firstBookingId)
                        .header("Authorization", bearer(firstResident.token())))
                .andExpect(status().isOk());

        Long secondBookingId = createBooking(secondResident, DESK_PLACE_ID, date);

        assertThat(secondBookingId).isNotEqualTo(firstBookingId);
        assertThat(bookingRepository.findById(secondBookingId).orElseThrow().getActive()).isTrue();
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

    private AuthMembership registerJoinAndTopUp(String email, Long amountMinorUnits) throws Exception {
        String token = registerAndGetToken(email);
        Long membershipId = join(token);
        topUp(membershipId, amountMinorUnits);
        return new AuthMembership(token, membershipId);
    }

    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "name", "Booking Resident",
                                "description", "booking flow"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private Long join(String token) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("membershipId").asLong();
    }

    private void topUp(Long membershipId, Long amountMinorUnits) throws Exception {
        mockMvc.perform(post("/api/internal/coworkings/{coworkingId}/memberships/{membershipId}/balance-adjustments", COWORKING_ID, membershipId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountMinorUnits", amountMinorUnits,
                                "comment", "Booking test top-up"
                        ))))
                .andExpect(status().isOk());
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

    private BookingContext bookingContext(
            List<BookingContext.ScheduleException> scheduleExceptions,
            List<BookingContext.PlaceClosing> placeClosings
    ) {
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
                                50,
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
                                50,
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
                scheduleExceptions,
                placeClosings
        );
    }

    private record AuthMembership(String token, Long membershipId) {
    }
}
