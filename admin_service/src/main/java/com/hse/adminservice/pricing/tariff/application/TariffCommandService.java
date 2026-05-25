package com.hse.adminservice.pricing.tariff.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
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
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffCommandService {
    private final TariffRepository tariffRepository;
    private final CoworkingRepository coworkingRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final AdminAuthorizationService authorizationService;
    private final TariffMapper tariffMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final TariffPayloadValidator tariffPayloadValidator;
    private final CoefficientNormalizer coefficientNormalizer;
    private final TimeProvider timeProvider;

    @Transactional
    public TariffResponse create(Long coworkingId, TariffCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
        String normalizedName = request.getName().trim();
        if (tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId, normalizedName)) {
            throw new ConflictException("Название тарифа должно быть уникальным в рамках коворкинга");
        }
        tariffPayloadValidator.validate(
                request.getPricePerDay(),
                request.getFullRefundHoursBefore(),
                request.getLateCancellationRefundPercent(),
                request.getCancellationCompensationCoefficient(),
                request.getDayClosureCompensationCoefficient(),
                request.getMembershipBlockCompensationCoefficient()
        );
        LocalDateTime now = timeProvider.now();
        Tariff tariff = Tariff.builder()
                .coworking(coworking)
                .name(normalizedName)
                .pricePerDay(request.getPricePerDay())
                .fullRefundHoursBefore(request.getFullRefundHoursBefore())
                .lateCancellationRefundPercent(request.getLateCancellationRefundPercent())
                .cancellationCompensationCoefficient(coefficientNormalizer.normalize(request.getCancellationCompensationCoefficient()))
                .dayClosureCompensationCoefficient(coefficientNormalizer.normalize(request.getDayClosureCompensationCoefficient()))
                .membershipBlockCompensationCoefficient(coefficientNormalizer.normalize(request.getMembershipBlockCompensationCoefficient()))
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Tariff saved = tariffRepository.save(tariff);
        configurationVersionService.bumpVersion(coworkingId);
        return tariffMapper.toResponse(saved);
    }

    @Transactional
    public TariffResponse update(Long coworkingId, Long tariffId, TariffUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Tariff tariff = getExistingTariff(coworkingId, tariffId);
        String normalizedName = request.getName().trim();
        if (!tariff.getName()
                .equalsIgnoreCase(normalizedName) && tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Название тарифа должно быть уникальным в рамках коворкинга");
        }
        tariffPayloadValidator.validate(
                request.getPricePerDay(),
                request.getFullRefundHoursBefore(),
                request.getLateCancellationRefundPercent(),
                request.getCancellationCompensationCoefficient(),
                request.getDayClosureCompensationCoefficient(),
                request.getMembershipBlockCompensationCoefficient()
        );
        tariff.setName(normalizedName);
        tariff.setPricePerDay(request.getPricePerDay());
        tariff.setFullRefundHoursBefore(request.getFullRefundHoursBefore());
        tariff.setLateCancellationRefundPercent(request.getLateCancellationRefundPercent());
        tariff.setCancellationCompensationCoefficient(coefficientNormalizer.normalize(request.getCancellationCompensationCoefficient()));
        tariff.setDayClosureCompensationCoefficient(coefficientNormalizer.normalize(request.getDayClosureCompensationCoefficient()));
        tariff.setMembershipBlockCompensationCoefficient(coefficientNormalizer.normalize(request.getMembershipBlockCompensationCoefficient()));
        if (request.getActive() != null) {
            tariff.setActive(request.getActive());
        }
        tariff.setVersion((tariff.getVersion() == null ? 0 : tariff.getVersion()) + 1);
        tariff.setUpdatedAt(timeProvider.now());
        Tariff saved = tariffRepository.save(tariff);
        configurationVersionService.bumpVersion(coworkingId);
        return tariffMapper.toResponse(saved);
    }

    @Transactional
    public void archive(Long coworkingId, Long tariffId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Tariff tariff = getExistingTariff(coworkingId, tariffId);
        boolean used = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .anyMatch(placeType -> placeType.getTariff().getId().equals(tariffId));
        if (used) {
            throw new ConflictException("Нельзя архивировать тариф, пока он используется типами мест");
        }
        LocalDateTime now = timeProvider.now();
        tariff.setArchived(true);
        tariff.setActive(false);
        tariff.setArchivedAt(now);
        tariff.setVersion((tariff.getVersion() == null ? 0 : tariff.getVersion()) + 1);
        tariff.setUpdatedAt(now);
        tariffRepository.save(tariff);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private Tariff getExistingTariff(Long coworkingId, Long tariffId) {
        return tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(tariffId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Тариф не найден"));
    }
}
