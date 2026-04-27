package com.hse.adminservice.pricing.tariff.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.discount.domain.TariffDiscountRule;
import com.hse.adminservice.pricing.discount.dto.TariffDiscountRuleRequest;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.dto.TariffCreateRequest;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.dto.TariffUpdateRequest;
import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffServiceImpl implements TariffService {
    private final TariffRepository tariffRepository;
    private final CoworkingRepository coworkingRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final AdminAuthorizationService authorizationService;
    private final TariffMapper tariffMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;

    @Override
    @Transactional
    public TariffResponse create(Long coworkingId, TariffCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        String normalizedName = request.getName().trim();
        if (tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId, normalizedName)) {
            throw new ConflictException("Tariff name must be unique within coworking");
        }
        validateTariffPayload(
                request.getPricePerDay(),
                request.getMinBookingDays(),
                request.getFullRefundHoursBefore(),
                request.getLateCancellationRefundPercent(),
                request.getCancellationCompensationCoefficient(),
                request.getDayClosureCompensationCoefficient(),
                request.getMembershipBlockCompensationCoefficient(),
                request.getDiscountRules()
        );
        LocalDateTime now = LocalDateTime.now();
        Tariff tariff = Tariff.builder()
                .coworking(coworking)
                .name(normalizedName)
                .pricePerDay(request.getPricePerDay())
                .minBookingDays(request.getMinBookingDays())
                .fullRefundHoursBefore(request.getFullRefundHoursBefore())
                .lateCancellationRefundPercent(request.getLateCancellationRefundPercent())
                .cancellationCompensationCoefficient(normalizeCoefficient(request.getCancellationCompensationCoefficient()))
                .dayClosureCompensationCoefficient(normalizeCoefficient(request.getDayClosureCompensationCoefficient()))
                .membershipBlockCompensationCoefficient(normalizeCoefficient(request.getMembershipBlockCompensationCoefficient()))
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        tariff.setDiscountRules(buildDiscountRules(tariff, request.getDiscountRules()));
        Tariff saved = tariffRepository.save(tariff);
        configurationVersionService.bumpVersion(coworkingId);
        return tariffMapper.toResponse(saved);
    }

    @Override
    public List<TariffResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_READ);
        return tariffRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream().map(
                tariffMapper::toResponse).toList();
    }

    @Override
    public TariffResponse getById(Long coworkingId, Long tariffId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_READ);
        return tariffMapper.toResponse(getExistingTariff(coworkingId, tariffId));
    }

    @Override
    @Transactional
    public TariffResponse update(Long coworkingId, Long tariffId, TariffUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Tariff tariff = getExistingTariff(coworkingId, tariffId);
        String normalizedName = request.getName().trim();
        if (!tariff.getName()
                .equalsIgnoreCase(normalizedName) && tariffRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Tariff name must be unique within coworking");
        }
        validateTariffPayload(
                request.getPricePerDay(),
                request.getMinBookingDays(),
                request.getFullRefundHoursBefore(),
                request.getLateCancellationRefundPercent(),
                request.getCancellationCompensationCoefficient(),
                request.getDayClosureCompensationCoefficient(),
                request.getMembershipBlockCompensationCoefficient(),
                request.getDiscountRules()
        );
        tariff.setName(normalizedName);
        tariff.setPricePerDay(request.getPricePerDay());
        tariff.setMinBookingDays(request.getMinBookingDays());
        tariff.setFullRefundHoursBefore(request.getFullRefundHoursBefore());
        tariff.setLateCancellationRefundPercent(request.getLateCancellationRefundPercent());
        tariff.setCancellationCompensationCoefficient(normalizeCoefficient(request.getCancellationCompensationCoefficient()));
        tariff.setDayClosureCompensationCoefficient(normalizeCoefficient(request.getDayClosureCompensationCoefficient()));
        tariff.setMembershipBlockCompensationCoefficient(normalizeCoefficient(request.getMembershipBlockCompensationCoefficient()));
        tariff.getDiscountRules().clear();
        tariff.getDiscountRules().addAll(buildDiscountRules(tariff, request.getDiscountRules()));
        if (request.getActive() != null)
            tariff.setActive(request.getActive());
        tariff.setVersion((tariff.getVersion() == null ? 0 : tariff.getVersion()) + 1);
        tariff.setUpdatedAt(LocalDateTime.now());
        Tariff saved = tariffRepository.save(tariff);
        configurationVersionService.bumpVersion(coworkingId);
        return tariffMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long tariffId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.TARIFF_EDIT);
        Tariff tariff = getExistingTariff(coworkingId, tariffId);
        boolean used = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .anyMatch(placeType -> placeType.getTariff().getId().equals(tariffId));
        if (used) {
            throw new ConflictException("Cannot archive tariff while place types still reference it");
        }
        LocalDateTime now = LocalDateTime.now();
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
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found"));
    }

    private BigDecimal normalizeCoefficient(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    private void validateTariffPayload(
            Integer pricePerDay,
            Integer minBookingDays,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient,
            List<TariffDiscountRuleRequest> rules
    ) {
        if (pricePerDay == null || pricePerDay < 0) {
            throw new ConflictException("Price per day must be zero or greater");
        }
        if (minBookingDays == null || minBookingDays < 1) {
            throw new ConflictException("Min booking days must be at least 1");
        }
        if (fullRefundHoursBefore == null || fullRefundHoursBefore < 0) {
            throw new ConflictException("Full refund hours before must be zero or greater");
        }
        if (lateCancellationRefundPercent == null || lateCancellationRefundPercent < 0 || lateCancellationRefundPercent > 100) {
            throw new ConflictException("Late cancellation refund percent must be between 0 and 100");
        }
        validateCoefficient("Cancellation compensation coefficient", cancellationCompensationCoefficient);
        validateCoefficient("Day closure compensation coefficient", dayClosureCompensationCoefficient);
        validateCoefficient("Membership block compensation coefficient", membershipBlockCompensationCoefficient);
        validateDiscountRules(rules == null ? List.of() : rules);
    }

    private void validateCoefficient(String label, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(label + " must be zero or greater");
        }
    }

    private void validateDiscountRules(List<TariffDiscountRuleRequest> rules) {
        Integer previousThreshold = null;
        Integer previousDiscount = null;
        for (TariffDiscountRuleRequest rule : rules) {
            if (rule.getThresholdQuantity() == null || rule.getDiscountPercent() == null) {
                throw new ConflictException("Every discount rule must include threshold quantity and discount percent");
            }
            if (rule.getThresholdQuantity() < 1) {
                throw new ConflictException("Discount threshold quantity must be at least 1");
            }
            if (rule.getDiscountPercent() < 0 || rule.getDiscountPercent() > 100) {
                throw new ConflictException("Discount percent must be between 0 and 100");
            }
            if (previousThreshold != null && rule.getThresholdQuantity() <= previousThreshold) {
                throw new ConflictException("Discount rules must be ordered by strictly increasing threshold quantity");
            }
            if (previousDiscount != null && rule.getDiscountPercent() <= previousDiscount) {
                throw new ConflictException("Discount percent must strictly increase as threshold quantity grows");
            }
            previousThreshold = rule.getThresholdQuantity();
            previousDiscount = rule.getDiscountPercent();
        }
    }

    private List<TariffDiscountRule> buildDiscountRules(Tariff tariff, List<TariffDiscountRuleRequest> rules) {
        List<TariffDiscountRuleRequest> safeRules = rules == null ? List.of() : rules;
        return safeRules.stream().map(rule -> TariffDiscountRule.builder()
                .tariff(tariff)
                .thresholdQuantity(rule.getThresholdQuantity())
                .discountPercent(rule.getDiscountPercent())
                .build()).toList();
    }
}
