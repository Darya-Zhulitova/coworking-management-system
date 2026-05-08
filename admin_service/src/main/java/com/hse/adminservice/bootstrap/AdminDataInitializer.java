package com.hse.adminservice.bootstrap;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.discount.domain.TariffDiscountRule;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.application.AccessService;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import com.hse.adminservice.servicecatalog.domain.ServiceRequestType;
import com.hse.adminservice.servicecatalog.persistence.ServiceRequestTypeRepository;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AdminDataInitializer {
    private final TimeProvider timeProvider;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;
    private final SystemCoworkingRoleDefinitions systemCoworkingRoleDefinitions;

    @Bean
    CommandLineRunner initAdmin(
            AdminRepository adminRepository,
            CoworkingRepository coworkingRepository,
            RoleRepository roleRepository,
            FloorRepository floorRepository,
            TariffRepository tariffRepository,
            PlaceTypeRepository placeTypeRepository,
            PlaceRepository placeRepository,
            ServiceRequestTypeRepository serviceRequestTypeRepository
    ) {
        return args -> {
            LocalDateTime now = timeProvider.now();

            Admin admin = adminRepository.findByEmailIgnoreCase("admin@test.test").orElseGet(() -> adminRepository.save(
                    Admin.builder()
                            .email("admin@test.test")
                            .name("Артём")
                            .passwordHash(passwordEncoder.encode("pass"))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Admin secondAdmin = adminRepository.findByEmailIgnoreCase("manager@test.test")
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email("manager@test.test")
                            .name("Игорь")
                            .passwordHash(passwordEncoder.encode("pass"))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            adminRepository.findByEmailIgnoreCase("staff@test.test")
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email("staff@test.test")
                            .name("Мария")
                            .passwordHash(passwordEncoder.encode("pass"))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            adminRepository.findByEmailIgnoreCase("support@test.test")
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email("support@test.test")
                            .name("Дмитрий")
                            .passwordHash(passwordEncoder.encode("pass"))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Coworking coworking = findOrCreateCoworking(
                    coworkingRepository,
                    "Волга Хаб",
                    "Коворкинг в деловом районе Нижнего Новгорода для специалистов на удалёнке, проектных команд и предпринимателей. Пространство ориентировано на длительную аренду рабочих мест и небольших офисов.",
                    "Нижний Новгород, улица Новая, 2к1",
                    "Пн–Вс, 08:00–22:00",
                    "Гибкое рабочее пространство в центре города",
                    "Открытые рабочие зоны, переговорные комнаты, тихие кабинеты и стабильная инфраструктура для ежедневной работы.",
                    "[\"https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1497366412874-3415097a27e7?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=1600&q=80\"]",
                    admin.getId(),
                    true,
                    now
            );
            Coworking secondCoworking = findOrCreateCoworking(
                    coworkingRepository,
                    "Северный квартал",
                    "Современный коворкинг в центре Санкт-Петербурга для фрилансеров, команд и небольших студий. Подходит для спокойной ежедневной работы, встреч с клиентами и коротких командных сессий.",
                    "Санкт-Петербург, Лиговский проспект, 74",
                    "Пн–Сб, 09:00–21:00",
                    "Пространство для спокойной работы и небольших команд",
                    "Удобное расположение, переговорные комнаты, стабильный интернет, зоны для сосредоточенной работы и коротких встреч.",
                    "[\"https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1600&q=80\"]",
                    secondAdmin.getId(),
                    false,
                    now
            );
            Coworking thirdCoworking = findOrCreateCoworking(
                    coworkingRepository,
                    "Урал Точка",
                    "Деловое пространство в Екатеринбурге для командных встреч, интенсивов и проектной работы. Подходит для бронирования переговорных и командных комнат на день.",
                    "Екатеринбург, улица Малышева, 51",
                    "Пн–Пт, 10:00–20:00",
                    "Переговорные и командные комнаты в центре Екатеринбурга",
                    "Формат для встреч, презентаций, стратегических сессий и совместной работы небольших команд.",
                    "[\"https://images.unsplash.com/photo-1497366412874-3415097a27e7?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1600&q=80\",\"https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=1600&q=80\"]",
                    admin.getId(),
                    false,
                    now
            );

            ensureSystemRoles(roleRepository, coworking, now);
            ensureSystemRoles(roleRepository, secondCoworking, now);
            ensureSystemRoles(roleRepository, thirdCoworking, now);

            Role managerRole = roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworking.getId())
                    .stream()
                    .filter(role -> "Финансовый менеджер".equals(role.getName()))
                    .findFirst()
                    .orElseThrow();
            Role staffSupportRole = roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworking.getId())
                    .stream()
                    .filter(role -> "Офис-менеджер".equals(role.getName()))
                    .findFirst()
                    .orElseThrow();

            accessService.assignRole(coworking.getId(), "staff@test.test", managerRole.getId());
            accessService.assignRole(coworking.getId(), "support@test.test", staffSupportRole.getId());


            Floor firstFloor = seedFloor(floorRepository, coworking, "Открытая зона", null, now);
            Floor secondFloor = seedFloor(floorRepository, coworking, "Тихая зона", null, now);

            Tariff deskTariff = seedTariff(
                    tariffRepository,
                    coworking,
                    "Стандартный тариф",
                    100000,
                    1,
                    now,
                    List.of(rule(2, 5), rule(3, 10), rule(7, 15))
            );
            Tariff meetingTariff = seedTariff(
                    tariffRepository,
                    coworking,
                    "Бизнес тариф",
                    300000,
                    1,
                    now,
                    List.of(rule(2, 10))
            );
            Tariff quietTariff = seedTariff(
                    tariffRepository,
                    coworking,
                    "Премиум тариф",
                    650000,
                    1,
                    now,
                    List.of(rule(2, 5))
            );

            PlaceType deskType = seedPlaceType(
                    placeTypeRepository,
                    coworking,
                    deskTariff,
                    "Стандартное рабочее место",
                    now
            );
            PlaceType meetingType = seedPlaceType(
                    placeTypeRepository,
                    coworking,
                    meetingTariff,
                    "Переговорная комната",
                    now
            );
            PlaceType quietType = seedPlaceType(placeTypeRepository, coworking, quietTariff, "Кабинет", now);

            seedPlace(
                    placeRepository,
                    coworking,
                    firstFloor,
                    deskType,
                    "Стол A-1",
                    new BigDecimal("0.1500"),
                    new BigDecimal("0.2500"),
                    List.of("у окна", "монитор 27", "эргономичное кресло"),
                    true,
                    false,
                    now
            );
            seedPlace(
                    placeRepository,
                    coworking,
                    firstFloor,
                    meetingType,
                    "Переговорная Ока",
                    new BigDecimal("0.3000"),
                    new BigDecimal("0.2500"),
                    List.of("экран", "маркерная доска", "видеосвязь"),
                    true,
                    false,
                    now
            );
            seedPlace(
                    placeRepository,
                    coworking,
                    secondFloor,
                    quietType,
                    "Кабинет Витязь",
                    null,
                    null,
                    List.of("тишина", "настольная лампа", "звукоизоляция"),
                    true,
                    false,
                    now
            );

            seedServiceRequestType(serviceRequestTypeRepository, coworking, "Обращение", 0, now);
            seedServiceRequestType(serviceRequestTypeRepository, coworking, "Печать документов", 30000, now);
            seedServiceRequestType(serviceRequestTypeRepository, coworking, "Аренда повербанка на день", 50000, now);
            seedServiceRequestType(serviceRequestTypeRepository, secondCoworking, "Обращение", 0, now);
            seedServiceRequestType(serviceRequestTypeRepository, secondCoworking, "Печать документов", 30000, now);
            seedServiceRequestType(
                    serviceRequestTypeRepository,
                    secondCoworking,
                    "Аренда повербанка на день",
                    50000,
                    now
            );
            seedServiceRequestType(serviceRequestTypeRepository, thirdCoworking, "Обращение", 0, now);
            seedServiceRequestType(serviceRequestTypeRepository, thirdCoworking, "Печать документов", 30000, now);
            seedServiceRequestType(
                    serviceRequestTypeRepository,
                    thirdCoworking,
                    "Аренда повербанка на день",
                    50000,
                    now
            );

            coworkingRepository.save(coworking);
            coworkingRepository.save(secondCoworking);
            coworkingRepository.save(thirdCoworking);
        };
    }

    private Coworking findOrCreateCoworking(
            CoworkingRepository repository,
            String name,
            String description,
            String address,
            String workingHoursLabel,
            String heroTitle,
            String heroText,
            String imageUrlsJson,
            Long ownerId,
            boolean autoApproveMembership,
            LocalDateTime now
    ) {
        return repository.findAllByArchivedFalse()
                .stream()
                .filter(existing -> name.equals(existing.getName()))
                .findFirst()
                .map(existing -> {
                    existing.setDescription(description);
                    existing.setAddress(address);
                    existing.setWorkingHoursLabel(workingHoursLabel);
                    existing.setHeroTitle(heroTitle);
                    existing.setHeroText(heroText);
                    existing.setImageUrlsJson(imageUrlsJson);
                    existing.setOwnerId(ownerId);
                    existing.setAutoApproveMembership(autoApproveMembership);
                    existing.setActive(true);
                    existing.setUpdatedAt(now);
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(Coworking.builder()
                        .name(name)
                        .description(description)
                        .address(address)
                        .workingHoursLabel(workingHoursLabel)
                        .heroTitle(heroTitle)
                        .heroText(heroText)
                        .imageUrlsJson(imageUrlsJson)
                        .schedule(127)
                        .ownerId(ownerId)
                        .autoApproveMembership(autoApproveMembership)
                        .active(true)
                        .archived(false)
                        .configurationVersion(0L)
                        .archivedAt(null)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
    }

    private void ensureSystemRoles(RoleRepository repository, Coworking coworking, LocalDateTime now) {
        if (!repository.existsByCoworkingIdAndNameIgnoreCase(coworking.getId(), "Финансовый менеджер"))
            repository.save(systemCoworkingRoleDefinitions.buildManagerRole(coworking, now));
        if (!repository.existsByCoworkingIdAndNameIgnoreCase(coworking.getId(), "Офис-менеджер"))
            repository.save(systemCoworkingRoleDefinitions.buildStaffSupportRole(coworking, now));
    }

    private Floor seedFloor(
            FloorRepository repository,
            Coworking coworking,
            String name,
            String imageFileId,
            LocalDateTime now
    ) {
        return repository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworking.getId())
                .stream()
                .filter(floor -> name.equals(floor.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Floor floor = repository.save(Floor.builder()
                            .coworking(coworking)
                            .name(name)
                            .index(repository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworking.getId())
                                    .size())
                            .imageFileId(imageFileId)
                            .active(true)
                            .archived(false)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                    coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
                    return floor;
                });
    }

    private Tariff seedTariff(
            TariffRepository repository,
            Coworking coworking,
            String name,
            int pricePerDay,
            int minBookingDays,
            LocalDateTime now,
            List<DiscountRuleSeed> rules
    ) {
        Tariff tariff = repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId())
                .stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> repository.save(Tariff.builder()
                        .coworking(coworking)
                        .name(name)
                        .pricePerDay(pricePerDay)
                        .minBookingDays(minBookingDays)
                        .fullRefundHoursBefore(24)
                        .lateCancellationRefundPercent(25)
                        .cancellationCompensationCoefficient(new BigDecimal("1.0000"))
                        .dayClosureCompensationCoefficient(new BigDecimal("1.0000"))
                        .membershipBlockCompensationCoefficient(new BigDecimal("0.5000"))
                        .version(1)
                        .active(true)
                        .archived(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
        if (tariff.getDiscountRules().isEmpty()) {
            for (DiscountRuleSeed rule : rules) {
                TariffDiscountRule discountRule = new TariffDiscountRule();
                discountRule.setTariff(tariff);
                discountRule.setThresholdQuantity(rule.thresholdQuantity());
                discountRule.setDiscountPercent(rule.discountPercent());
                tariff.getDiscountRules().add(discountRule);
            }
            repository.save(tariff);
        }
        coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
        return tariff;
    }

    private PlaceType seedPlaceType(
            PlaceTypeRepository repository,
            Coworking coworking,
            Tariff tariff,
            String name,
            LocalDateTime now
    ) {
        return repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId())
                .stream()
                .filter(type -> name.equals(type.getName()))
                .findFirst()
                .orElseGet(() -> {
                    PlaceType placeType = repository.save(PlaceType.builder().coworking(coworking).tariff(tariff).name(
                            name).active(true).archived(false).createdAt(now).updatedAt(now).build());
                    coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
                    return placeType;
                });
    }

    private void seedPlace(
            PlaceRepository repository,
            Coworking coworking,
            Floor floor,
            PlaceType placeType,
            String name,
            BigDecimal locX,
            BigDecimal locY,
            List<String> amenities,
            boolean active,
            boolean archived,
            LocalDateTime now
    ) {
        Place place = repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId()).stream().filter(
                item -> name.equals(item.getName())).findFirst().orElseGet(() -> Place.builder()
                .coworking(coworking)
                .floor(floor)
                .placeType(placeType)
                .name(name)
                .createdAt(now)
                .build());
        place.setLocX(locX);
        place.setLocY(locY);
        place.setAmenitiesRaw(String.join(",", amenities));
        place.setActive(active);
        place.setArchived(archived);
        place.setArchivedAt(archived ? now : null);
        place.setUpdatedAt(now);
        repository.save(place);
        coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
    }

    private void seedServiceRequestType(
            ServiceRequestTypeRepository repository,
            Coworking coworking,
            String name,
            int cost,
            LocalDateTime now
    ) {
        boolean exists = repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId())
                .stream()
                .anyMatch(item -> name.equals(item.getName()));
        if (exists) {
            return;
        }
        repository.save(ServiceRequestType.builder()
                .coworking(coworking)
                .name(name)
                .cost(cost)
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
    }

    private DiscountRuleSeed rule(int thresholdQuantity, int discountPercent) {
        return new DiscountRuleSeed(thresholdQuantity, discountPercent);
    }

    private record DiscountRuleSeed(
            int thresholdQuantity,
            int discountPercent
    ) {
    }
}
