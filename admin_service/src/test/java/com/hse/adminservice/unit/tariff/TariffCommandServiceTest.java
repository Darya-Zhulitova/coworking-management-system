package com.hse.adminservice.unit.tariff;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.tariff.application.TariffCommandService;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.dto.TariffCreateRequest;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.dto.TariffUpdateRequest;
import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.pricing.tariff.validation.CoefficientNormalizer;
import com.hse.adminservice.pricing.tariff.validation.TariffPayloadValidator;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffCommandServiceTest {
    @Mock TariffRepository tariffRepository;
    @Mock CoworkingRepository coworkingRepository;
    @Mock PlaceTypeRepository placeTypeRepository;
    @Mock AdminAuthorizationService authorizationService;
    @Mock CoworkingConfigurationVersionService configurationVersionService;
    @Mock TimeProvider timeProvider;

    TariffCommandService service;

    @BeforeEach
    void setUp() {
        service = new TariffCommandService(
                tariffRepository,
                coworkingRepository,
                placeTypeRepository,
                authorizationService,
                new TariffMapper(),
                configurationVersionService,
                new TariffPayloadValidator(),
                new CoefficientNormalizer(),
                timeProvider
        );
    }

    @Test
    void createNormalizesNameAndCoefficientsAndBumpsConfigurationVersion() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 1, 12, 0);
        Coworking coworking = Coworking.builder().id(15L).build();
        when(coworkingRepository.findByIdAndArchivedFalse(15L)).thenReturn(Optional.of(coworking));
        when(tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(15L, "Flexible day"))
                .thenReturn(false);
        when(timeProvider.now()).thenReturn(now);
        when(tariffRepository.save(any(Tariff.class))).thenAnswer(invocation -> {
            Tariff saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        TariffResponse response = service.create(15L, createRequest("  Flexible day  "));

        verify(authorizationService).requireCoworkingAction(15L, Grant.TARIFF_EDIT);
        verify(configurationVersionService).bumpVersion(15L);
        ArgumentCaptor<Tariff> captor = ArgumentCaptor.forClass(Tariff.class);
        verify(tariffRepository).save(captor.capture());
        Tariff saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Flexible day");
        assertThat(saved.getCancellationCompensationCoefficient()).isEqualByComparingTo("1.25");
        assertThat(saved.getDayClosureCompensationCoefficient()).isEqualByComparingTo("0");
        assertThat(saved.getMembershipBlockCompensationCoefficient()).isEqualByComparingTo("0.75");
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getArchived()).isFalse();
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("Flexible day");
    }

    @Test
    void createRejectsDuplicateNameBeforeSaving() {
        Coworking coworking = Coworking.builder().id(15L).build();
        when(coworkingRepository.findByIdAndArchivedFalse(15L)).thenReturn(Optional.of(coworking));
        when(tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(15L, "Daily"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(15L, createRequest(" Daily ")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Название тарифа должно быть уникальным в рамках коворкинга");

        verify(tariffRepository, never()).save(any());
        verify(configurationVersionService, never()).bumpVersion(15L);
    }

    @Test
    void updateChangesRulesIncrementsVersionAndKeepsActiveWhenRequestActiveIsNull() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 2, 12, 0);
        Coworking coworking = Coworking.builder().id(15L).build();
        Tariff tariff = existingTariff(55L, coworking, "Old name", 3, true);
        when(tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(55L, 15L)).thenReturn(Optional.of(tariff));
        when(tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(15L, "New name"))
                .thenReturn(false);
        when(timeProvider.now()).thenReturn(updatedAt);
        when(tariffRepository.save(tariff)).thenReturn(tariff);

        TariffUpdateRequest request = updateRequest(" New name ", null);
        TariffResponse response = service.update(15L, 55L, request);

        verify(authorizationService).requireCoworkingAction(15L, Grant.TARIFF_EDIT);
        verify(configurationVersionService).bumpVersion(15L);
        assertThat(tariff.getName()).isEqualTo("New name");
        assertThat(tariff.getVersion()).isEqualTo(4);
        assertThat(tariff.getActive()).isTrue();
        assertThat(tariff.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.version()).isEqualTo(4);
    }

    @Test
    void archiveRejectsTariffStillReferencedByPlaceType() {
        Coworking coworking = Coworking.builder().id(15L).build();
        Tariff tariff = existingTariff(55L, coworking, "Daily", 1, true);
        PlaceType placeType = PlaceType.builder().id(3L).tariff(tariff).build();
        when(tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(55L, 15L)).thenReturn(Optional.of(tariff));
        when(placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(15L)).thenReturn(List.of(placeType));

        assertThatThrownBy(() -> service.archive(15L, 55L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Нельзя архивировать тариф, пока он используется типами мест");

        verify(tariffRepository, never()).save(any());
        verify(configurationVersionService, never()).bumpVersion(15L);
    }

    @Test
    void archiveMarksTariffInactiveArchivedAndBumpsVersionWhenUnused() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 3, 12, 0);
        Coworking coworking = Coworking.builder().id(15L).build();
        Tariff tariff = existingTariff(55L, coworking, "Daily", 2, true);
        when(tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(55L, 15L)).thenReturn(Optional.of(tariff));
        when(placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(15L)).thenReturn(List.of());
        when(timeProvider.now()).thenReturn(now);

        service.archive(15L, 55L);

        assertThat(tariff.getArchived()).isTrue();
        assertThat(tariff.getActive()).isFalse();
        assertThat(tariff.getArchivedAt()).isEqualTo(now);
        assertThat(tariff.getUpdatedAt()).isEqualTo(now);
        assertThat(tariff.getVersion()).isEqualTo(3);
        verify(tariffRepository).save(tariff);
        verify(configurationVersionService).bumpVersion(15L);
    }

    @Test
    void updateThrowsNotFoundForArchivedOrForeignTariff() {
        when(tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(55L, 15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(15L, 55L, updateRequest("Daily", true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Тариф не найден");
    }

    private TariffCreateRequest createRequest(String name) {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setName(name);
        request.setPricePerDay(2500L);
        request.setFullRefundHoursBefore(24);
        request.setLateCancellationRefundPercent(50);
        request.setCancellationCompensationCoefficient(new BigDecimal("1.2500"));
        request.setDayClosureCompensationCoefficient(null);
        request.setMembershipBlockCompensationCoefficient(new BigDecimal("0.7500"));
        return request;
    }

    private TariffUpdateRequest updateRequest(String name, Boolean active) {
        TariffUpdateRequest request = new TariffUpdateRequest();
        request.setName(name);
        request.setPricePerDay(3000L);
        request.setFullRefundHoursBefore(48);
        request.setLateCancellationRefundPercent(60);
        request.setCancellationCompensationCoefficient(new BigDecimal("1.1000"));
        request.setDayClosureCompensationCoefficient(new BigDecimal("1.2500"));
        request.setMembershipBlockCompensationCoefficient(new BigDecimal("0.5000"));
        request.setActive(active);
        return request;
    }

    private Tariff existingTariff(Long id, Coworking coworking, String name, Integer version, Boolean active) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        return Tariff.builder()
                .id(id)
                .coworking(coworking)
                .name(name)
                .pricePerDay(2000L)
                .fullRefundHoursBefore(24)
                .lateCancellationRefundPercent(50)
                .cancellationCompensationCoefficient(BigDecimal.ONE)
                .dayClosureCompensationCoefficient(BigDecimal.ONE)
                .membershipBlockCompensationCoefficient(BigDecimal.ONE)
                .version(version)
                .active(active)
                .archived(false)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
