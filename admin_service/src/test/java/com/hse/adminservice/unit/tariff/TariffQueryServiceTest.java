package com.hse.adminservice.unit.tariff;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.pricing.tariff.application.TariffQueryService;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffQueryServiceTest {
    @Mock TariffRepository tariffRepository;
    @Mock AdminAuthorizationService authorizationService;

    TariffQueryService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new TariffQueryService(tariffRepository, authorizationService, new TariffMapper());
    }

    @Test
    void getAllRequiresReadGrantAndMapsRepositoryResult() {
        Coworking coworking = Coworking.builder().id(10L).build();
        Tariff alpha = tariff(1L, coworking, "Alpha");
        Tariff beta = tariff(2L, coworking, "Beta");
        when(tariffRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(10L)).thenReturn(List.of(alpha, beta));

        List<TariffResponse> result = service.getAll(10L);

        verify(authorizationService).requireCoworkingAction(10L, Grant.TARIFF_READ);
        assertThat(result).extracting(TariffResponse::name).containsExactly("Alpha", "Beta");
    }

    @Test
    void getByIdRequiresReadGrantAndThrowsWhenTariffDoesNotBelongToCoworking() {
        when(tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Тариф не найден");

        verify(authorizationService).requireCoworkingAction(10L, Grant.TARIFF_READ);
    }

    private Tariff tariff(Long id, Coworking coworking, String name) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);
        return Tariff.builder()
                .id(id)
                .coworking(coworking)
                .name(name)
                .pricePerDay(1000L)
                .fullRefundHoursBefore(24)
                .lateCancellationRefundPercent(50)
                .cancellationCompensationCoefficient(BigDecimal.ONE)
                .dayClosureCompensationCoefficient(BigDecimal.ONE)
                .membershipBlockCompensationCoefficient(BigDecimal.ONE)
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
