package com.hse.adminservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import com.hse.adminservice.support.*;
import com.hse.adminservice.testinfra.AdminServiceTestConfiguration;
import com.hse.adminservice.testinfra.FakeUserOperationsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AdminServiceTestConfiguration.class)
@Transactional
class AdminSecurityAndInternalApiSmokeTest {
    private static final String INTERNAL_API_KEY = "test-internal-key";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdminRepository adminRepository;
    @Autowired CoworkingRepository coworkingRepository;
    @Autowired TariffRepository tariffRepository;
    @Autowired FloorRepository floorRepository;
    @Autowired PlaceTypeRepository placeTypeRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired AccessRepository accessRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FakeUserOperationsClient fakeUserOperationsClient;

    @Test
    void internalConfigurationApiRequiresInternalApiKeyAndReturnsBookingContextWhenKeyIsValid() throws Exception {
        var fixture = createCoworkingFixture(true, Grant.USER_READ);

        mockMvc.perform(get("/api/internal/coworkings/{coworkingId}/booking-context", fixture.coworking().getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/coworkings/{coworkingId}/booking-context", fixture.coworking().getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coworkingId").value(fixture.coworking().getId()))
                .andExpect(jsonPath("$.floors[0].id").value(fixture.floorId()))
                .andExpect(jsonPath("$.tariffs[0].id").value(fixture.tariffId()))
                .andExpect(jsonPath("$.placeTypes[0].id").value(fixture.placeTypeId()))
                .andExpect(jsonPath("$.places[0].id").value(fixture.placeId()));
    }

    @Test
    void protectedOperationsApiRequiresJwtAndDelegatesToUserOperationsClientWhenAdminHasAccess() throws Exception {
        var fixture = createCoworkingFixture(false, Grant.USER_READ);

        mockMvc.perform(get("/api/coworkings/{coworkingId}/operations-dashboard", fixture.coworking().getId()))
                .andExpect(status().isForbidden());

        String token = login(fixture.admin().getEmail(), "secret123");

        mockMvc.perform(get("/api/coworkings/{coworkingId}/operations-dashboard", fixture.coworking().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queues.pendingMemberships").value(2));

        assertThat(fakeUserOperationsClient.calls())
                .contains("getOperationsDashboard:%d".formatted(fixture.coworking().getId()));
    }

    @Test
    void protectedOperationsApiRejectsAuthenticatedAdminWithoutRequiredCoworkingAccess() throws Exception {
        var allowedFixture = createCoworkingFixture(false, Grant.USER_READ);
        var anotherCoworking = coworkingRepository.save(CoworkingTestFactory.coworking(999_999L));
        String token = login(allowedFixture.admin().getEmail(), "secret123");

        mockMvc.perform(get("/api/coworkings/{coworkingId}/operations-dashboard", anotherCoworking.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private Fixture createCoworkingFixture(boolean owner, Grant... grants) {
        Admin admin = adminRepository.save(AdminTestFactory.admin(passwordEncoder.encode("secret123")));
        Coworking coworking = coworkingRepository.save(CoworkingTestFactory.coworking(owner ? admin.getId() : 777_777L));
        var tariff = tariffRepository.save(TariffTestFactory.tariff(coworking));
        var floor = floorRepository.save(FloorTestFactory.floor(coworking));
        var placeType = placeTypeRepository.save(PlaceTypeTestFactory.placeType(coworking, tariff));
        var place = placeRepository.save(PlaceTestFactory.place(coworking, floor, placeType));
        if (!owner) {
            var role = roleRepository.save(RbacTestFactory.role(coworking, RbacTestFactory.grants(grants)));
            accessRepository.save(RbacTestFactory.access(admin, coworking, role));
        }
        return new Fixture(admin, coworking, floor.getId(), tariff.getId(), placeType.getId(), place.getId());
    }


    private record Fixture(
            Admin admin,
            Coworking coworking,
            Long floorId,
            Long tariffId,
            Long placeTypeId,
            Long placeId
    ) {
    }
}
