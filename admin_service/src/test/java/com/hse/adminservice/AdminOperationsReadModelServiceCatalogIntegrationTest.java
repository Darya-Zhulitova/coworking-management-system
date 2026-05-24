package com.hse.adminservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import com.hse.adminservice.servicecatalog.persistence.ServiceRequestTypeRepository;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import com.hse.adminservice.support.AdminTestFactory;
import com.hse.adminservice.support.CoworkingTestFactory;
import com.hse.adminservice.support.FloorTestFactory;
import com.hse.adminservice.support.PlaceTestFactory;
import com.hse.adminservice.support.PlaceTypeTestFactory;
import com.hse.adminservice.support.RbacTestFactory;
import com.hse.adminservice.support.TariffTestFactory;
import com.hse.adminservice.testinfra.AdminServiceTestConfiguration;
import com.hse.adminservice.testinfra.FakeUserOperationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AdminServiceTestConfiguration.class)
@Transactional
class AdminOperationsReadModelServiceCatalogIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdminRepository adminRepository;
    @Autowired CoworkingRepository coworkingRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired AccessRepository accessRepository;
    @Autowired TariffRepository tariffRepository;
    @Autowired FloorRepository floorRepository;
    @Autowired PlaceTypeRepository placeTypeRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired ServiceRequestTypeRepository serviceRequestTypeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FakeUserOperationsClient fakeUserOperationsClient;

    @BeforeEach
    void resetFake() {
        fakeUserOperationsClient.clear();
    }

    @Test
    void operationsReadModelReadsUserDomainQueuesDashboardProfilesAndOperationalPlaces() throws Exception {
        Fixture fixture = ownerFixture();
        String token = login(fixture.admin().getEmail(), "secret123");
        Place place = createOperationalPlace(fixture.coworking());
        fakeUserOperationsClient.setProfileBookingPlaceId(place.getId());

        mockMvc.perform(get("/api/coworkings/{coworkingId}/users", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(501))
                .andExpect(jsonPath("$[0].name").value("Resident One"));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/memberships", fixture.coworking().getId())
                        .param("search", "resident")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].membershipId").value(601))
                .andExpect(jsonPath("$[0].balanceMinorUnits").value(25000));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/memberships/{membershipId}", fixture.coworking().getId(), 601L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(601))
                .andExpect(jsonPath("$.activeBookings[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$.activeBookings[0].placeName").value(place.getName()));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/operations-dashboard", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queues.pendingMemberships").value(2))
                .andExpect(jsonPath("$.finance.totalBalance").value(100000))
                .andExpect(jsonPath("$.occupancy.monthlyPercent").value(67));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/membership-requests", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/pay-requests", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payRequestId").value(801));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/service-requests", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceRequestId").value(901))
                .andExpect(jsonPath("$[0].cost").value(2000));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/places/operational", fixture.coworking().getId())
                        .param("floorId", place.getFloor().getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$[0].totalBookings").value(2))
                .andExpect(jsonPath("$[0].unfinishedBookings").value(1));

        assertThat(fakeUserOperationsClient.calls()).contains(
                "getUsers:%d".formatted(fixture.coworking().getId()),
                "getMembershipList:%d:resident".formatted(fixture.coworking().getId()),
                "getMembershipProfile:%d:601".formatted(fixture.coworking().getId()),
                "getOperationsDashboard:%d".formatted(fixture.coworking().getId()),
                "getMemberships:%d".formatted(fixture.coworking().getId()),
                "getPayRequests:%d".formatted(fixture.coworking().getId()),
                "getServiceRequests:%d".formatted(fixture.coworking().getId()),
                "getPlaceBookings:%d:%d".formatted(fixture.coworking().getId(), place.getId())
        );
    }

    @Test
    void adminCanProcessUserDomainRequestsAndServiceDeskMessages() throws Exception {
        Fixture fixture = ownerFixture();
        String token = login(fixture.admin().getEmail(), "secret123");

        mockMvc.perform(post("/api/coworkings/{coworkingId}/membership-requests/{membershipId}/decision", fixture.coworking().getId(), 601L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decision", "APPROVE", "comment", "Welcome"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/pay-requests/{payRequestId}/decision", fixture.coworking().getId(), 801L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decision", "APPROVE", "comment", "Paid"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/pay-requests/{payRequestId}/decision", fixture.coworking().getId(), 802L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decision", "REJECT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Укажите комментарий администратора при отклонении платежной заявки."));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/service-requests/{serviceRequestId}/messages", fixture.coworking().getId(), 901L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "We are checking it"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorType").value("ADMIN"))
                .andExpect(jsonPath("$.text").value("We are checking it"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
        mockMvc.perform(multipart("/api/coworkings/{coworkingId}/service-requests/{serviceRequestId}/messages", fixture.coworking().getId(), 901L)
                        .file(file)
                        .param("text", "Attached photo")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0].fileName").value("photo.png"));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/service-requests/{serviceRequestId}/workspace", fixture.coworking().getId(), 901L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.serviceRequestId").value(901))
                .andExpect(jsonPath("$.messages[0].text").value("Please clean the room"))
                .andExpect(jsonPath("$.availableActions[0]").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision", fixture.coworking().getId(), 901L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decision", "RESOLVE", "comment", "Done"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/memberships/{membershipId}/balance-adjustments", fixture.coworking().getId(), 601L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amountMinorUnits", 5000, "comment", "Manual correction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinorUnits").value(30000));

        assertThat(fakeUserOperationsClient.calls()).contains(
                "decideMembership:%d:601:APPROVE:Welcome".formatted(fixture.coworking().getId()),
                "decidePayRequest:%d:801:APPROVE:Paid".formatted(fixture.coworking().getId()),
                "addServiceRequestMessage:%d:901:We are checking it:no-file".formatted(fixture.coworking().getId()),
                "addServiceRequestMessage:%d:901:Attached photo:photo.png".formatted(fixture.coworking().getId()),
                "getServiceRequestWorkspace:%d:901".formatted(fixture.coworking().getId()),
                "decideServiceRequest:%d:901:RESOLVE:Done".formatted(fixture.coworking().getId()),
                "adjustMembershipBalance:%d:601:5000:Manual correction".formatted(fixture.coworking().getId())
        );
    }

    @Test
    void serviceRequestTypeCatalogSupportsCrudValidationAndRbac() throws Exception {
        Fixture fixture = ownerFixture();
        String ownerToken = login(fixture.admin().getEmail(), "secret123");

        Long typeId = createServiceRequestType(ownerToken, fixture.coworking().getId(), "Cleaning", 2_000L);

        mockMvc.perform(get("/api/coworkings/{coworkingId}/service-request-types/{typeId}", fixture.coworking().getId(), typeId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cleaning"))
                .andExpect(jsonPath("$.cost").value(2000));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/service-request-types", fixture.coworking().getId())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/coworkings/{coworkingId}/service-request-types/{typeId}", fixture.coworking().getId(), typeId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Deep cleaning", "cost", 3_000L, "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Deep cleaning"))
                .andExpect(jsonPath("$.cost").value(3000))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/service-request-types", fixture.coworking().getId())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Deep cleaning", "cost", 1_000L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Название типа сервисной заявки должно быть уникальным в рамках коворкинга"));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/service-request-types", fixture.coworking().getId())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Invalid", "cost", -1L))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/coworkings/{coworkingId}/service-request-types/{typeId}", fixture.coworking().getId(), typeId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
        assertThat(serviceRequestTypeRepository.findById(typeId)).isPresent().get().satisfies(type -> {
            assertThat(type.getArchived()).isTrue();
            assertThat(type.getActive()).isFalse();
        });

        String readerToken = createStaffAndLogin(fixture.coworking(), Grant.SERVICE_REQUEST_TYPE_READ);
        mockMvc.perform(get("/api/coworkings/{coworkingId}/service-request-types", fixture.coworking().getId())
                        .header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/coworkings/{coworkingId}/service-request-types", fixture.coworking().getId())
                        .header("Authorization", bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Forbidden", "cost", 500L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userDomainBusinessAndInfrastructureErrorsArePropagatedThroughAdminApi() throws Exception {
        Fixture fixture = ownerFixture();
        String token = login(fixture.admin().getEmail(), "secret123");

        fakeUserOperationsClient.failMembershipProfileWith(new ResourceNotFoundException("Membership not found in User Domain"));
        mockMvc.perform(get("/api/coworkings/{coworkingId}/memberships/{membershipId}", fixture.coworking().getId(), 404L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Membership not found in User Domain"));

        fakeUserOperationsClient.clear();
        fakeUserOperationsClient.failUsersWith(new ConflictException("User Domain rejected operation"));
        mockMvc.perform(get("/api/coworkings/{coworkingId}/users", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User Domain rejected operation"));

        fakeUserOperationsClient.clear();
        fakeUserOperationsClient.failDashboardWith(new IllegalStateException("Не удалось получить данные пользователей."));
        mockMvc.perform(get("/api/coworkings/{coworkingId}/operations-dashboard", fixture.coworking().getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Не удалось получить данные пользователей."));
    }

    private Fixture ownerFixture() {
        Admin admin = createAdmin("owner-" + UUID.randomUUID() + "@example.test", "secret123");
        Coworking coworking = coworkingRepository.save(CoworkingTestFactory.coworking(admin.getId()));
        return new Fixture(admin, coworking);
    }

    private Admin createAdmin(String email, String password) {
        Admin admin = AdminTestFactory.admin(passwordEncoder.encode(password));
        admin.setEmail(email);
        return adminRepository.save(admin);
    }

    private String createStaffAndLogin(Coworking coworking, Grant... grants) throws Exception {
        String email = "staff-" + UUID.randomUUID() + "@example.test";
        Admin staff = createAdmin(email, "secret123");
        Role role = roleRepository.save(RbacTestFactory.role(coworking, Set.of(grants)));
        accessRepository.save(RbacTestFactory.access(staff, coworking, role));
        return login(email, "secret123");
    }

    private Place createOperationalPlace(Coworking coworking) {
        Tariff tariff = tariffRepository.save(TariffTestFactory.tariff(coworking));
        Floor floor = floorRepository.save(FloorTestFactory.floor(coworking));
        PlaceType placeType = placeTypeRepository.save(PlaceTypeTestFactory.placeType(coworking, tariff));
        return placeRepository.save(PlaceTestFactory.place(coworking, floor, placeType));
    }

    private Long createServiceRequestType(String token, Long coworkingId, String name, Long cost) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/service-request-types", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "cost", cost))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(Admin admin, Coworking coworking) {
    }
}
