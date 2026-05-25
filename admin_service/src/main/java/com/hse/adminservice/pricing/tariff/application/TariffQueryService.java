package com.hse.adminservice.pricing.tariff.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffQueryService {
    private final TariffRepository tariffRepository;
    private final AdminAuthorizationService authorizationService;
    private final TariffMapper tariffMapper;

    public List<TariffResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_READ);
        return tariffRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream().map(
                tariffMapper::toResponse).toList();
    }

    public TariffResponse getById(Long coworkingId, Long tariffId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_READ);
        return tariffMapper.toResponse(getExistingTariff(coworkingId, tariffId));
    }

    Tariff getExistingTariff(Long coworkingId, Long tariffId) {
        return tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(tariffId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Тариф не найден"));
    }
}
