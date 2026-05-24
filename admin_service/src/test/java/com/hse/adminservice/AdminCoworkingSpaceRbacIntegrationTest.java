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
import com.hse.adminservice.support.AdminTestFactory;
import com.hse.adminservice.support.CoworkingTestFactory;
import com.hse.adminservice.support.RbacTestFactory;
import com.hse.adminservice.support.TariffTestFactory;
import com.hse.adminservice.testinfra.AdminServiceTestConfiguration;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AdminServiceTestConfiguration.class)
@Transactional
class AdminCoworkingSpaceRbacIntegrationTest {

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


    @Test
    void ownerCanCreateAndEditCoworkingSpaceModelAndUploadFloorPlan() throws Exception {
        String token = createAdminAndLogin();

        Long coworkingId = createCoworking(token, "Volga Hub " + UUID.randomUUID());

        mockMvc.perform(put("/api/coworkings/{id}", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Updated Volga Hub",
                                "description", "Updated description",
                                "address", "Nizhny Novgorod, Minina 1",
                                "workingHoursLabel", "08:00-22:00",
                                "heroTitle", "Updated hero",
                                "heroText", "Updated text",
                                "imageUrls", List.of("https://example.test/coworking.png"),
                                "active", true,
                                "autoApproveMembership", true,
                                "floorMapEnabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coworkingId))
                .andExpect(jsonPath("$.name").value("Updated Volga Hub"))
                .andExpect(jsonPath("$.autoApproveMembership").value(true))
                .andExpect(jsonPath("$.floorMapEnabled").value(true));

        Long floorId = createFloor(token, coworkingId, "First floor");

        MockMultipartFile plan = new MockMultipartFile(
                "file",
                "plan.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/coworkings/{coworkingId}/floors/{floorId}/plan", coworkingId, floorId)
                        .file(plan)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(floorId))
                .andExpect(jsonPath("$.imageFileId").value("images/floors/%d/test/full.jpg".formatted(floorId)));

        Long tariffId = tariffRepository.save(TariffTestFactory.tariff(coworkingRepository.getReferenceById(coworkingId))).getId();
        Long placeTypeId = createPlaceType(token, coworkingId, tariffId, "Desk");
        Long placeId = createPlace(token, coworkingId, floorId, placeTypeId, "Desk A1");

        mockMvc.perform(put("/api/coworkings/{coworkingId}/place-types/{placeTypeId}", coworkingId, placeTypeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Silent desk", "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Silent desk"));

        mockMvc.perform(put("/api/coworkings/{coworkingId}/places/{placeId}", coworkingId, placeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Desk A1 updated",
                                "locX", new BigDecimal("0.7000"),
                                "locY", new BigDecimal("0.2500"),
                                "imageFileId", "place-image-updated",
                                "amenities", List.of("monitor", "coffee", "window"),
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Desk A1 updated"));

        assertThat(coworkingRepository.findById(coworkingId)).isPresent().get().satisfies(coworking -> {
            assertThat(coworking.getName()).isEqualTo("Updated Volga Hub");
            assertThat(coworking.getConfigurationVersion()).isGreaterThan(0L);
        });
        assertThat(floorRepository.findById(floorId)).isPresent().get().satisfies(floor ->
                assertThat(floor.getImageFileId()).isEqualTo("images/floors/%d/test/full.jpg".formatted(floorId)));
        assertThat(placeTypeRepository.findById(placeTypeId)).isPresent().get().satisfies(placeType ->
                assertThat(placeType.getName()).isEqualTo("Silent desk"));
        assertThat(placeRepository.findById(placeId)).isPresent().get().satisfies(place -> {
            assertThat(place.getName()).isEqualTo("Desk A1 updated");
            assertThat(place.getAmenitiesRaw()).contains("monitor", "coffee", "window");
        });
    }

    @Test
    void ownerCanCreateRolesAssignStaffAndStaffCanOnlyUseGrantedPermissions() throws Exception {
        String ownerToken = createAdminAndLogin();
        Long coworkingId = createCoworking(ownerToken, "RBAC Hub " + UUID.randomUUID());
        Long floorId = createFloor(ownerToken, coworkingId, "Main floor");
        Long tariffId = tariffRepository.save(TariffTestFactory.tariff(coworkingRepository.getReferenceById(coworkingId))).getId();
        Long placeTypeId = createPlaceType(ownerToken, coworkingId, tariffId, "Open space desk");

        String staffEmail = "staff-" + UUID.randomUUID() + "@example.test";
        createAdmin(staffEmail, "secret123");

        Long readerRoleId = createRole(ownerToken, coworkingId, "Space reader", Grant.COWORKING_READ, Grant.FLOOR_READ, Grant.PLACE_TYPE_READ, Grant.PLACE_READ);
        Long editorRoleId = createRole(ownerToken, coworkingId, "Place editor", Grant.COWORKING_READ, Grant.FLOOR_READ, Grant.PLACE_TYPE_READ, Grant.PLACE_EDIT);

        Long accessId = assignRole(ownerToken, coworkingId, staffEmail, readerRoleId);
        String staffToken = login(staffEmail, "secret123");

        mockMvc.perform(get("/api/coworkings/{coworkingId}/floors", coworkingId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/places", coworkingId)
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeBody(floorId, placeTypeId, "Forbidden place")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/coworkings/{coworkingId}/staff/{accessId}", coworkingId, accessId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("roleId", editorRoleId, "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(editorRoleId));

        Long createdPlaceId = createPlace(staffToken, coworkingId, floorId, placeTypeId, "Allowed staff place");
        assertThat(placeRepository.findById(createdPlaceId)).isPresent().get().satisfies(place ->
                assertThat(place.getName()).isEqualTo("Allowed staff place"));
    }

    @Test
    void staffWithoutEditRightsCannotMutateCoworkingOrSpaceModel() throws Exception {
        Admin owner = createAdmin("owner-" + UUID.randomUUID() + "@example.test", "secret123");
        Admin staff = createAdmin("readonly-" + UUID.randomUUID() + "@example.test", "secret123");
        Coworking coworking = coworkingRepository.save(CoworkingTestFactory.coworking(owner.getId()));
        var role = roleRepository.save(RbacTestFactory.role(coworking, RbacTestFactory.grants(Grant.COWORKING_READ, Grant.FLOOR_READ)));
        accessRepository.save(RbacTestFactory.access(staff, coworking, role));
        String staffToken = login(staff.getEmail(), "secret123");

        mockMvc.perform(put("/api/coworkings/{id}", coworking.getId())
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Forbidden update",
                                "description", "Forbidden",
                                "address", "Forbidden",
                                "workingHoursLabel", "10:00-18:00",
                                "heroTitle", "No",
                                "heroText", "No",
                                "imageUrls", List.of(),
                                "active", true,
                                "autoApproveMembership", false,
                                "floorMapEnabled", false
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/floors", coworking.getId())
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Forbidden floor", "imageFileId", "forbidden-plan"))))
                .andExpect(status().isForbidden());
    }

    private Long createCoworking(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "description", "A coworking managed in tests",
                                "address", "Nizhny Novgorod",
                                "workingHoursLabel", "09:00-21:00",
                                "heroTitle", "Work here",
                                "heroText", "Reliable workspace",
                                "imageUrls", List.of(),
                                "autoApproveMembership", false,
                                "floorMapEnabled", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createFloor(String token, Long coworkingId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/floors", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "imageFileId", "initial-plan"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createPlaceType(String token, Long coworkingId, Long tariffId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/place-types", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "tariffId", tariffId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createPlace(String token, Long coworkingId, Long floorId, Long placeTypeId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/places", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeBody(floorId, placeTypeId, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createRole(String token, Long coworkingId, String name, Grant... grants) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/staff/roles", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "grants", Arrays.stream(grants).map(Enum::name).toList(),
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("roleId").asLong();
    }

    private Long assignRole(String token, Long coworkingId, String email, Long roleId) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/staff", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "roleId", roleId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleId").value(roleId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessId").asLong();
    }

    private String placeBody(Long floorId, Long placeTypeId, String name) throws Exception {
        return json(Map.of(
                "name", name,
                "floorId", floorId,
                "placeTypeId", placeTypeId,
                "locX", new BigDecimal("0.4500"),
                "locY", new BigDecimal("0.5500"),
                "imageFileId", "place-image-test",
                "amenities", List.of("monitor", "coffee")
        ));
    }

    private String createAdminAndLogin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.test";
        createAdmin(email, "secret123");
        return login(email, "secret123");
    }

    private Admin createAdmin(String email, String password) {
        Admin admin = AdminTestFactory.admin(passwordEncoder.encode(password));
        admin.setEmail(email);
        return adminRepository.save(admin);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
