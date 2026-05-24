package com.hse.userservice.feature.balance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.repository.PayRequestRepository;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.hse.userservice.feature.membership.domain.MembershipStatus.PENDING;
import static com.hse.userservice.support.builder.LedgerEntryTestBuilder.ledgerEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BalanceAndPayRequestIntegrationTest extends IntegrationTestSupport {
    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PayRequestRepository payRequestRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private UnitsService unitsService;

    @Test
    void payRequestIsCreatedByResidentAndApprovedByInternalApiIntoLedgerEntry() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("pay-approve"));

        Long payRequestId = createPayRequest(authMembership.token(), authMembership.membershipId(), 5000L, "Top up");
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .isEmpty();

        mockMvc.perform(post("/api/internal/coworkings/1/pay-requests/{payRequestId}/decision", payRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVE", "comment", "payment received"))))
                .andExpect(status().isNoContent());

        PayRequest approved = payRequestRepository.findById(payRequestId).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(PayRequestStatus.APPROVED);
        assertThat(approved.getAdminComment()).isEqualTo("payment received");

        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(5000L);
        LedgerEntry entry = ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId())
                .getFirst();
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.BALANCE_TOP_UP);
        assertThat(entry.getAmount()).isEqualTo(5000L);
        assertThat(entry.getReferenceId()).isEqualTo(payRequestId);

        mockMvc.perform(get("/api/memberships/{membershipId}/balance", authMembership.membershipId())
                        .header("Authorization", "Bearer " + authMembership.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinorUnits").value(5000L))
                .andExpect(jsonPath("$.ledger[0].type").value("BALANCE_TOP_UP"))
                .andExpect(jsonPath("$.payRequests[0].status").value("APPROVED"));
    }

    @Test
    void payRequestCanBeRejectedByInternalApiWithoutChangingLedgerBalance() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("pay-reject"));
        Long payRequestId = createPayRequest(authMembership.token(), authMembership.membershipId(), 1000L, "Top up rejected");

        mockMvc.perform(post("/api/internal/coworkings/1/pay-requests/{payRequestId}/decision", payRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "REJECT", "comment", "receipt not found"))))
                .andExpect(status().isNoContent());

        PayRequest rejected = payRequestRepository.findById(payRequestId).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(PayRequestStatus.REJECTED);
        assertThat(rejected.getAdminComment()).isEqualTo("receipt not found");
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isZero();
        assertThat(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId()))
                .isEmpty();
    }

    @Test
    void withdrawalPayRequestCannotMakeLedgerBalanceNegative() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("pay-negative"));
        Long payRequestId = createPayRequest(authMembership.token(), authMembership.membershipId(), -1000L, "Withdraw too much");

        mockMvc.perform(post("/api/internal/coworkings/1/pay-requests/{payRequestId}/decision", payRequestId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVE", "comment", "withdraw"))))
                .andExpect(status().isConflict());

        PayRequest stillPending = payRequestRepository.findById(payRequestId).orElseThrow();
        assertThat(stillPending.getStatus()).isEqualTo(PayRequestStatus.PENDING);
        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isZero();
    }

    @Test
    void activeAndBlockedMembershipsCanCreatePayRequestsButPendingMembershipCannot() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("pay-status"));

        createPayRequest(authMembership.token(), authMembership.membershipId(), 100L, "Active membership request");

        Membership membership = membershipRepository.findById(authMembership.membershipId()).orElseThrow();
        membership.setStatus(com.hse.userservice.feature.membership.domain.MembershipStatus.BLOCKED);
        membershipRepository.saveAndFlush(membership);
        createPayRequest(authMembership.token(), authMembership.membershipId(), 200L, "Blocked membership request");

        membership.setStatus(PENDING);
        membershipRepository.saveAndFlush(membership);
        mockMvc.perform(post("/api/memberships/{membershipId}/pay-requests", authMembership.membershipId())
                        .header("Authorization", "Bearer " + authMembership.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 300L,
                                "userComment", "Pending membership request"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void balanceIsCalculatedFromImmutableLedgerEntriesAndNotStoredOnMembership() throws Exception {
        Long membershipId = registerAndJoin(uniqueEmail("ledger-balance")).membershipId();
        ledgerEntryRepository.save(ledgerEntry()
                .membershipId(membershipId)
                .amount(5000L)
                .type(LedgerEntryType.BALANCE_TOP_UP)
                .referenceId(101L)
                .build());
        ledgerEntryRepository.save(ledgerEntry()
                .membershipId(membershipId)
                .amount(-1250L)
                .type(LedgerEntryType.BOOKING_CHARGE)
                .referenceId(102L)
                .build());
        ledgerEntryRepository.save(ledgerEntry()
                .membershipId(membershipId)
                .amount(250L)
                .type(LedgerEntryType.BOOKING_USER_CANCELLATION_REFUND)
                .referenceId(103L)
                .build());

        assertThat(unitsService.getBalanceMinorUnits(membershipId)).isEqualTo(4000L);
        assertThat(Arrays.stream(Membership.class.getDeclaredFields()).map(Field::getName).toList())
                .doesNotContain("balance", "balanceMinorUnits", "amount");
    }

    @Test
    void internalManualBalanceAdjustmentCreatesLedgerEntryInsteadOfMutatingMembershipBalance() throws Exception {
        AuthMembership authMembership = registerAndJoin(uniqueEmail("manual-adjustment"));

        mockMvc.perform(post("/api/internal/coworkings/1/memberships/{membershipId}/balance-adjustments", authMembership.membershipId())
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amountMinorUnits", 1500L,
                                "comment", "Manual credit"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinorUnits").value(1500L));

        assertThat(unitsService.getBalanceMinorUnits(authMembership.membershipId())).isEqualTo(1500L);
        LedgerEntry entry = ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(authMembership.membershipId())
                .getFirst();
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.MANUAL_CREDIT);
        assertThat(entry.getAmount()).isEqualTo(1500L);
        assertThat(entry.getReferenceId()).isLessThan(0L);
    }

    private Long createPayRequest(String token, Long membershipId, Long amount, String comment) throws Exception {
        String response = mockMvc.perform(post("/api/memberships/{membershipId}/pay-requests", membershipId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", amount,
                                "userComment", comment
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private AuthMembership registerAndJoin(String email) throws Exception {
        String token = registerAndGetToken(email);
        String response = mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long membershipId = objectMapper.readTree(response).get("membershipId").asLong();
        return new AuthMembership(token, membershipId);
    }


    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "name", "Balance Resident",
                                "description", "balance flow"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    private record AuthMembership(String token, Long membershipId) {
    }
}
