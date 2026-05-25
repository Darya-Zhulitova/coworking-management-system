package com.hse.adminservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
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
import com.hse.adminservice.testinfra.FakeUserBookingImpactPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AdminServiceTestConfiguration.class)
class AdminTariffScheduleIntegrationTest {

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
    @Autowired CoworkingScheduleExceptionRepository scheduleExceptionRepository;
    @Autowired PlaceClosingRepository placeClosingRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FakeUserBookingImpactPort fakeUserBookingImpactPort;

    @BeforeEach
    void resetTestState() {
        fakeUserBookingImpactPort.clear();
        placeClosingRepository.deleteAll();
        scheduleExceptionRepository.deleteAll();
        placeRepository.deleteAll();
        placeTypeRepository.deleteAll();
        floorRepository.deleteAll();
        tariffRepository.deleteAll();
        accessRepository.deleteAll();
        roleRepository.deleteAll();
        coworkingRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    void ownerCanCreateReadUpdateAndArchiveTariffWithDiscountAndCompensationRules() throws Exception {
        Fixture fixture = createOwnedCoworking();

        Long tariffId = createTariff(fixture.token(), fixture.coworking().getId(), "Flexible day");

        mockMvc.perform(get("/api/coworkings/{coworkingId}/tariffs/{tariffId}", fixture.coworking().getId(), tariffId)
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Flexible day"))
                .andExpect(jsonPath("$.pricePerDay").value(2500))
                .andExpect(jsonPath("$.lateCancellationRefundPercent").value(40))
                .andExpect(jsonPath("$.cancellationCompensationCoefficient").value(1.25))
                .andExpect(jsonPath("$.dayClosureCompensationCoefficient").value(1.50))
                .andExpect(jsonPath("$.membershipBlockCompensationCoefficient").value(0.75));

        mockMvc.perform(put("/api/coworkings/{coworkingId}/tariffs/{tariffId}", fixture.coworking().getId(), tariffId)
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody("Updated flexible day", 3_000L, 48, 60,
                                "1.1000", "1.2500", "0.5000", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated flexible day"))
                .andExpect(jsonPath("$.pricePerDay").value(3000))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/tariffs", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tariffId));

        mockMvc.perform(delete("/api/coworkings/{coworkingId}/tariffs/{tariffId}", fixture.coworking().getId(), tariffId)
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isNoContent());

        assertThat(tariffRepository.findById(tariffId)).isPresent().get().satisfies(tariff -> {
            assertThat(tariff.getArchived()).isTrue();
            assertThat(tariff.getActive()).isFalse();
            assertThat(tariff.getVersion()).isEqualTo(3);
        });
    }

    @Test
    void tariffValidationRejectsInvalidRuleValuesAndDuplicateNames() throws Exception {
        Fixture fixture = createOwnedCoworking();
        createTariff(fixture.token(), fixture.coworking().getId(), "Unique tariff");

        mockMvc.perform(post("/api/coworkings/{coworkingId}/tariffs", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody("Unique tariff", 2000L, 24, 50,
                                "1.0000", "1.0000", "1.0000", null))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/tariffs", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody("Invalid percent", 2000L, 24, 101,
                                "1.0000", "1.0000", "1.0000", null))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/tariffs", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody("Invalid coefficient", 2000L, 24, 50,
                                "-0.1000", "1.0000", "1.0000", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleUpdatePreviewAndCommitUseUserDomainImpactContract() throws Exception {
        Fixture fixture = createOwnedCoworking();

        mockMvc.perform(get("/api/coworkings/{coworkingId}/schedule", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monday").value(true))
                .andExpect(jsonPath("$.sunday").value(true));

        Map<String, Object> weekdaysOnly = scheduleBody(true, true, true, true, true, false, false, null);
        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/preview", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(weekdaysOnly)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("PREVIEW_SCHEDULE_REDUCTION"));

        Map<String, Object> commitBody = scheduleBody(true, true, true, true, true, false, false, "schedule-impact-1");
        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/commit", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(commitBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("schedule-impact-1"));

        assertThat(fakeUserBookingImpactPort.calls())
                .anyMatch(call -> call.startsWith("previewForScheduleReduction:%d:".formatted(fixture.coworking().getId())))
                .anyMatch(call -> call.startsWith("commitScheduleReduction:%d:".formatted(fixture.coworking().getId())) && call.endsWith(":schedule-impact-1"));
        assertThat(coworkingRepository.findById(fixture.coworking().getId())).isPresent().get().satisfies(coworking -> {
            assertThat(coworking.getSchedule()).isNotEqualTo(127);
            assertThat(coworking.getConfigurationVersion()).isGreaterThan(fixture.coworking().getConfigurationVersion());
        });
    }

    @Test
    void calendarExceptionsAndCloseDayCreateConfigurationStateAndNotifyUserDomain() throws Exception {
        Fixture fixture = createOwnedCoworking();
        LocalDate openDate = LocalDate.now().plusDays(10);
        LocalDate closeDate = LocalDate.now().plusDays(11);

        String exceptionResponse = mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/exceptions", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("date", openDate.toString(), "type", "OPEN", "name", "Special open day"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long exceptionId = objectMapper.readTree(exceptionResponse).get("id").asLong();

        mockMvc.perform(get("/api/coworkings/{coworkingId}/schedule/exceptions", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exceptionId));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/exceptions", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("date", closeDate.toString(), "type", "CLOSE", "name", "Wrong close"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/close-day/preview", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("date", closeDate.toString(), "name", "Maintenance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("PREVIEW_CLOSE_DAY"));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/close-day/commit", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("date", closeDate.toString(), "name", "Maintenance", "impactHash", "close-day-impact-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("close-day-impact-1"));

        assertThat(scheduleExceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(fixture.coworking().getId()))
                .anySatisfy(item -> {
                    assertThat(item.getDate()).isEqualTo(openDate);
                    assertThat(item.getType()).isEqualTo(ScheduleExceptionType.OPEN);
                })
                .anySatisfy(item -> {
                    assertThat(item.getDate()).isEqualTo(closeDate);
                    assertThat(item.getType()).isEqualTo(ScheduleExceptionType.CLOSE);
                });
        assertThat(fakeUserBookingImpactPort.calls())
                .contains("previewForCloseDay:%d:%s:Maintenance".formatted(fixture.coworking().getId(), closeDate))
                .contains("commitCloseDay:%d:%s:Maintenance:close-day-impact-1".formatted(fixture.coworking().getId(), closeDate));
    }

    @Test
    void placeClosingPreviewAndCommitCreateClosingAndUseUserDomainCommand() throws Exception {
        Fixture fixture = createOwnedCoworking();
        Place place = createPlaceGraph(fixture.coworking());
        LocalDate closingDate = LocalDate.now().plusDays(12);

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/closings/preview", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("placeId", place.getId(), "date", closingDate.toString(), "name", "Desk repair"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("PREVIEW_PLACE_CLOSING"));

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/closings/commit", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("placeId", place.getId(), "date", closingDate.toString(), "name", "Desk repair", "impactHash", "place-closing-impact-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactHash").value("place-closing-impact-1"));

        mockMvc.perform(get("/api/coworkings/{coworkingId}/schedule/closings", fixture.coworking().getId())
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$[0].date").value(closingDate.toString()));

        assertThat(placeClosingRepository.findAll()).anySatisfy(closing -> {
            assertThat(closing.getPlace().getId()).isEqualTo(place.getId());
            assertThat(closing.getDate()).isEqualTo(closingDate);
            assertThat(closing.getName()).isEqualTo("Desk repair");
            assertThat(closing.getArchived()).isFalse();
        });
        assertThat(fakeUserBookingImpactPort.calls())
                .contains("previewForPlaceClosing:%d:%s:Desk repair".formatted(place.getId(), closingDate))
                .contains("commitPlaceClosing:%d:%s:Desk repair:place-closing-impact-1".formatted(place.getId(), closingDate));
    }

    @Test
    void scheduleAndTariffOperationsAreForbiddenWithoutEditGrants() throws Exception {
        Admin owner = createAdmin("owner-" + UUID.randomUUID() + "@example.test", "secret123");
        Admin staff = createAdmin("schedule-reader-" + UUID.randomUUID() + "@example.test", "secret123");
        Coworking coworking = coworkingRepository.save(CoworkingTestFactory.coworking(owner.getId()));
        var role = roleRepository.save(RbacTestFactory.role(coworking, RbacTestFactory.grants(Grant.TARIFF_READ, Grant.SCHEDULE_READ)));
        accessRepository.save(RbacTestFactory.access(staff, coworking, role));
        String staffToken = login(staff.getEmail(), "secret123");

        mockMvc.perform(get("/api/coworkings/{coworkingId}/tariffs", coworking.getId())
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/tariffs", coworking.getId())
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody("Forbidden tariff", 2_000L, 24, 50,
                                "1.0000", "1.0000", "1.0000", null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/coworkings/{coworkingId}/schedule/commit", coworking.getId())
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleBody(true, true, true, true, true, false, false, "forbidden-impact"))))
                .andExpect(status().isForbidden());
    }

    private Fixture createOwnedCoworking() throws Exception {
        Admin owner = createAdmin("owner-" + UUID.randomUUID() + "@example.test", "secret123");
        Coworking coworking = coworkingRepository.save(CoworkingTestFactory.coworking(owner.getId()));
        return new Fixture(owner, coworking, login(owner.getEmail(), "secret123"));
    }

    private Place createPlaceGraph(Coworking coworking) {
        var tariff = tariffRepository.save(TariffTestFactory.tariff(coworking));
        Floor floor = floorRepository.save(FloorTestFactory.floor(coworking));
        PlaceType placeType = placeTypeRepository.save(PlaceTypeTestFactory.placeType(coworking, tariff));
        return placeRepository.save(PlaceTestFactory.place(coworking, floor, placeType));
    }

    private Long createTariff(String token, Long coworkingId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/coworkings/{coworkingId}/tariffs", coworkingId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tariffBody(name, 2_500L, 24, 40,
                                "1.2500", "1.5000", "0.7500", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Map<String, Object> tariffBody(
            String name,
            Long pricePerDay,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            String cancellationCompensationCoefficient,
            String dayClosureCompensationCoefficient,
            String membershipBlockCompensationCoefficient,
            Boolean active
    ) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("pricePerDay", pricePerDay);
        body.put("fullRefundHoursBefore", fullRefundHoursBefore);
        body.put("lateCancellationRefundPercent", lateCancellationRefundPercent);
        body.put("cancellationCompensationCoefficient", new BigDecimal(cancellationCompensationCoefficient));
        body.put("dayClosureCompensationCoefficient", new BigDecimal(dayClosureCompensationCoefficient));
        body.put("membershipBlockCompensationCoefficient", new BigDecimal(membershipBlockCompensationCoefficient));
        if (active != null) {
            body.put("active", active);
        }
        return body;
    }

    private Map<String, Object> scheduleBody(
            boolean monday,
            boolean tuesday,
            boolean wednesday,
            boolean thursday,
            boolean friday,
            boolean saturday,
            boolean sunday,
            String impactHash
    ) {
        var body = new LinkedHashMap<String, Object>();
        body.put("monday", monday);
        body.put("tuesday", tuesday);
        body.put("wednesday", wednesday);
        body.put("thursday", thursday);
        body.put("friday", friday);
        body.put("saturday", saturday);
        body.put("sunday", sunday);
        if (impactHash != null) {
            body.put("impactHash", impactHash);
        }
        return body;
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

    private record Fixture(Admin owner, Coworking coworking, String token) {
    }
}
