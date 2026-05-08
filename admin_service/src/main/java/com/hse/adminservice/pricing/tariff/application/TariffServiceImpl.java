package com.hse.adminservice.pricing.tariff.application;

import com.hse.adminservice.pricing.tariff.dto.TariffCreateRequest;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.dto.TariffUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffServiceImpl implements TariffService {
    private final TariffCommandService tariffCommandService;
    private final TariffQueryService tariffQueryService;

    @Override
    @Transactional
    public TariffResponse create(Long coworkingId, TariffCreateRequest request) {
        return tariffCommandService.create(coworkingId, request);
    }

    @Override
    public List<TariffResponse> getAll(Long coworkingId) {
        return tariffQueryService.getAll(coworkingId);
    }

    @Override
    public TariffResponse getById(Long coworkingId, Long tariffId) {
        return tariffQueryService.getById(coworkingId, tariffId);
    }

    @Override
    @Transactional
    public TariffResponse update(Long coworkingId, Long tariffId, TariffUpdateRequest request) {
        return tariffCommandService.update(coworkingId, tariffId, request);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long tariffId) {
        tariffCommandService.archive(coworkingId, tariffId);
    }
}
