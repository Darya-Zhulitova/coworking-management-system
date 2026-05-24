package com.hse.adminservice;

import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(AdminServiceTestConfiguration.class)
@Transactional
class AdminTestDataBuilderSmokeTest {
    @Autowired AdminRepository adminRepository;
    @Autowired CoworkingRepository coworkingRepository;
    @Autowired TariffRepository tariffRepository;
    @Autowired FloorRepository floorRepository;
    @Autowired PlaceTypeRepository placeTypeRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired AccessRepository accessRepository;
    @Autowired CoworkingScheduleExceptionRepository scheduleExceptionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void buildersCreatePersistableAdminCoworkingSpacePricingScheduleAndAccessGraph() {
        var admin = adminRepository.save(AdminTestFactory.admin(passwordEncoder.encode("secret123")));
        var coworking = coworkingRepository.save(CoworkingTestFactory.coworking(admin.getId()));
        var tariff = tariffRepository.save(TariffTestFactory.tariff(coworking));
        var floor = floorRepository.save(FloorTestFactory.floor(coworking));
        var placeType = placeTypeRepository.save(PlaceTypeTestFactory.placeType(coworking, tariff));
        var place = placeRepository.save(PlaceTestFactory.place(coworking, floor, placeType));
        var role = roleRepository.save(RbacTestFactory.role(coworking, RbacTestFactory.grants(Grant.USER_READ, Grant.USER_EDIT)));
        var access = accessRepository.save(RbacTestFactory.access(admin, coworking, role));
        var exception = scheduleExceptionRepository.save(ScheduleTestFactory.closedDay(coworking, LocalDate.now().plusDays(1)));

        assertThat(admin.getId()).isNotNull();
        assertThat(coworking.getOwnerId()).isEqualTo(admin.getId());
        assertThat(tariff.getCoworking().getId()).isEqualTo(coworking.getId());
        assertThat(floor.getCoworking().getId()).isEqualTo(coworking.getId());
        assertThat(placeType.getTariff().getId()).isEqualTo(tariff.getId());
        assertThat(place.getFloor().getId()).isEqualTo(floor.getId());
        assertThat(access.getRole().getId()).isEqualTo(role.getId());
        assertThat(exception.getCoworking().getId()).isEqualTo(coworking.getId());
    }
}
