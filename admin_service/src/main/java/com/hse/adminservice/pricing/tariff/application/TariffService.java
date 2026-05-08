package com.hse.adminservice.pricing.tariff.application;

import com.hse.adminservice.pricing.tariff.dto.TariffCreateRequest;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.dto.TariffUpdateRequest;

import java.util.List;

public interface TariffService {
    TariffResponse create(Long coworkingId, TariffCreateRequest request);

    List<TariffResponse> getAll(Long coworkingId);

    TariffResponse getById(Long coworkingId, Long tariffId);

    TariffResponse update(Long coworkingId, Long tariffId, TariffUpdateRequest request);

    void archive(Long coworkingId, Long tariffId);
}
