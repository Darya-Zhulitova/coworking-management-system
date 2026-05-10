package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.dto.request.BookingCartItemRequestDto;
import com.hse.userservice.dto.response.CartCalculationResponseDto;
import com.hse.userservice.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CartPricingService {
    private final BalanceService balanceService;
    private final BookingSnapshotContextFactory contextFactory;
    private final BookingPlaceResolver placeResolver;
    private final BookingAvailabilityService availabilityService;

    public CalculatedCart calculate(
            Long coworkingId,
            Membership membership,
            CoworkingConfigSnapshot snapshot,
            List<BookingCartItemRequestDto> rawItems
    ) {
        BookingSnapshotContext context = contextFactory.from(snapshot);
        List<BookingCartItemRequestDto> items = normalizeItems(rawItems);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one unique item.");
        }

        Map<String, Boolean> reservedPairs = availabilityService.findReservedPairs(items);
        Map<Long, Integer> quantityByTariffId = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        for (BookingCartItemRequestDto item : items) {
            ResolvedPlace resolvedPlace = placeResolver.resolve(item.placeId(), context, coworkingId);
            quantityByTariffId.merge(resolvedPlace.tariff().id(), 1, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : quantityByTariffId.entrySet()) {
            CoworkingConfigSnapshot.Tariff tariff = context.tariffsById().get(entry.getKey());
            if (tariff == null) {
                validationErrors.add("Tariff configuration is incomplete.");
                continue;
            }
            if (entry.getValue() < tariff.minBookingDays()) {
                validationErrors.add("Tariff \"" + tariff.name() + "\" requires at least " + tariff.minBookingDays() + " booking day(s).");
            }
        }

        List<ResolvedCartItem> resolvedItems = new ArrayList<>();
        for (BookingCartItemRequestDto item : items) {
            ResolvedPlace resolvedPlace = placeResolver.resolve(item.placeId(), context, coworkingId);
            int quantity = quantityByTariffId.getOrDefault(resolvedPlace.tariff().id(), 0);
            int discountPercent = resolveDiscountPercent(resolvedPlace.tariff(), quantity);
            long basePrice = resolvedPlace.tariff().pricePerDay();
            long discountAmount = Math.floorDiv(basePrice * discountPercent, 100);
            boolean available = availabilityService.isPlaceAvailable(
                    item.date(),
                    resolvedPlace.place(),
                    context,
                    reservedPairs
            );

            resolvedItems.add(new ResolvedCartItem(
                    item.placeId(),
                    resolvedPlace.place().name(),
                    item.date(),
                    resolvedPlace.floor().name(),
                    resolvedPlace.placeType().name(),
                    resolvedPlace.tariff(),
                    basePrice,
                    discountPercent,
                    discountAmount,
                    basePrice - discountAmount,
                    available
            ));
        }

        long totalBasePrice = resolvedItems.stream().mapToLong(ResolvedCartItem::basePrice).sum();
        long totalDiscount = resolvedItems.stream().mapToLong(ResolvedCartItem::discountAmount).sum();
        long totalFinalPrice = resolvedItems.stream().mapToLong(ResolvedCartItem::finalPrice).sum();
        int unavailableCount = (int) resolvedItems.stream().filter(item -> !item.available()).count();
        long currentBalance = balanceService.getBalanceMinorUnits(membership.getId());
        long balanceAfter = currentBalance - totalFinalPrice;
        boolean hasEnoughBalance = balanceAfter >= 0;
        List<String> discountHints = buildDiscountHints(quantityByTariffId, context);
        boolean canCheckout = validationErrors.isEmpty() && unavailableCount == 0 && hasEnoughBalance;

        CartCalculationResponseDto response = new CartCalculationResponseDto(
                coworkingId, resolvedItems.stream().map(item -> new CartCalculationResponseDto.CartCalculatedItemDto(
                item.placeId(),
                item.placeName(),
                item.date(),
                item.floorName(),
                item.typeName(),
                item.tariff().id(),
                item.basePrice(),
                item.discountPercent(),
                item.discountAmount(),
                item.finalPrice(),
                item.available()
        )).toList(), new CartCalculationResponseDto.CartSummaryDto(
                totalBasePrice,
                totalDiscount,
                totalFinalPrice,
                unavailableCount,
                discountHints,
                validationErrors,
                hasEnoughBalance,
                balanceAfter,
                canCheckout
        )
        );

        return new CalculatedCart(response, resolvedItems);
    }

    private List<BookingCartItemRequestDto> normalizeItems(List<BookingCartItemRequestDto> items) {
        LinkedHashMap<String, BookingCartItemRequestDto> unique = new LinkedHashMap<>();
        for (BookingCartItemRequestDto item : items) {
            unique.put(BookingSnapshotContextFactory.key(item.placeId(), item.date()), item);
        }
        return new ArrayList<>(unique.values());
    }

    private int resolveDiscountPercent(CoworkingConfigSnapshot.Tariff tariff, int quantity) {
        return tariff.discountRules().stream().filter(rule -> quantity >= rule.thresholdQuantity()).mapToInt(
                CoworkingConfigSnapshot.TariffDiscountRule::discountPercent).max().orElse(0);
    }

    private List<String> buildDiscountHints(Map<Long, Integer> quantityByTariffId, BookingSnapshotContext context) {
        List<String> hints = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityByTariffId.entrySet()) {
            CoworkingConfigSnapshot.Tariff tariff = context.tariffsById().get(entry.getKey());
            if (tariff == null) {
                continue;
            }
            tariff.discountRules()
                    .stream()
                    .filter(rule -> entry.getValue() < rule.thresholdQuantity())
                    .min(Comparator.comparing(CoworkingConfigSnapshot.TariffDiscountRule::thresholdQuantity))
                    .ifPresent(rule -> hints.add("Бронирований по тарифу \"" + tariff.name() + "\" до скидки " + rule.discountPercent() + "%: " + (rule.thresholdQuantity() - entry.getValue())));
        }
        return hints;
    }
}
