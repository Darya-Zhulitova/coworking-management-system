package com.hse.userservice.feature.membership;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.support.IntegrationTestSupport;
import com.hse.userservice.support.config.TestAdminServiceClientConfig.FakeAdminServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipFlowIntegrationTest extends IntegrationTestSupport {
    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeAdminServiceClient adminServiceClient;

    @Autowired
    private MembershipRepository membershipRepository;

    @BeforeEach
    void resetFakeAdmin() {
        adminServiceClient.reset();
    }

    @Test
    void joinByTokenCreatesActiveMembershipWhenCoworkingAutoApproves() throws Exception {
        String token = registerAndGetToken(uniqueEmail("active-membership"));

        String response = mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coworkingId").value(1L))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.existingMembership").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long membershipId = objectMapper.readTree(response).get("membershipId").asLong();
        Membership saved = membershipRepository.findById(membershipId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(saved.getApprovedAt()).isNotNull();
        assertThat(saved.getBlockedAt()).isNull();

        mockMvc.perform(post("/api/coworkings/join/test-join-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.existingMembership").value(true));
    }

    @Test
    void pendingMembershipCanBeApprovedThroughInternalApi() throws Exception {
        String joinToken = "manual-approval-token-" + UUID.randomUUID();
        adminServiceClient.putCoworking(manualApprovalCoworking(2L));
        adminServiceClient.putJoinToken(joinToken, 2L);
        String token = registerAndGetToken(uniqueEmail("pending-membership"));

        Long membershipId = joinAndReadMembershipId(token, joinToken, "pending");

        mockMvc.perform(post("/api/internal/coworkings/2/membership-requests/{membershipId}/decision", membershipId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVE", "comment", "ok"))))
                .andExpect(status().isNoContent());

        Membership approved = membershipRepository.findById(membershipId).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getBlockedAt()).isNull();
    }

    @Test
    void pendingMembershipCanBeRejectedThroughInternalApi() throws Exception {
        String joinToken = "manual-reject-token-" + UUID.randomUUID();
        adminServiceClient.putCoworking(manualApprovalCoworking(3L));
        adminServiceClient.putJoinToken(joinToken, 3L);
        String token = registerAndGetToken(uniqueEmail("rejected-membership"));

        Long membershipId = joinAndReadMembershipId(token, joinToken, "pending");

        mockMvc.perform(post("/api/internal/coworkings/3/membership-requests/{membershipId}/decision", membershipId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "REJECT", "comment", "not accepted"))))
                .andExpect(status().isNoContent());

        Membership rejected = membershipRepository.findById(membershipId).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(MembershipStatus.BLOCKED);
        assertThat(rejected.getApprovedAt()).isNull();
        assertThat(rejected.getBlockedAt()).isNotNull();
    }

    @Test
    void membershipSummaryUsesAdminReadModelAndLedgerBalance() throws Exception {
        String token = registerAndGetToken(uniqueEmail("membership-summary"));
        Long membershipId = joinAndReadMembershipId(token, "test-join-token", "active");

        mockMvc.perform(get("/api/users/me/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(membershipId))
                .andExpect(jsonPath("$[0].coworkingName").value("Test Coworking"))
                .andExpect(jsonPath("$[0].status").value("active"))
                .andExpect(jsonPath("$[0].balance").value(0));
    }

    private Long joinAndReadMembershipId(String authToken, String joinToken, String expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/join/{joinToken}", joinToken)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("membershipId").asLong();
    }

    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "secret123",
                                "name", "Membership Resident",
                                "description", "membership flow"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private CoworkingInfo manualApprovalCoworking(Long id) {
        return new CoworkingInfo(
                id,
                "Manual Coworking " + id,
                "Requires admin approval",
                "Manual address",
                "10:00-20:00",
                "Manual join",
                "Wait for approval",
                List.of(),
                false,
                true,
                true
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }
}
