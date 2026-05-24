package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import com.hse.userservice.feature.booking.dto.CartCalculationResponseDto;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.integration.dto.BookingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartPricingService {
    private final UnitsService unitsService;
    private final BookingSnapshotContextFactory contextFactory;
    private final BookingPlaceResolver placeResolver;
    private final BookingAvailabilityService availabilityService;
    private final CartValidationPolicy validationPolicy;
    private final CartItemNormalizer cartItemNormalizer;

    public CalculatedCart calculate(
            Long coworkingId,
            Membership membership,
            BookingContext snapshot,
            List<BookingCartItemRequestDto> rawItems
    ) {
        BookingSnapshotContext context = contextFactory.from(snapshot);
        List<BookingCartItemRequestDto> items = cartItemNormalizer.normalize(rawItems);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Корзина должна содержать хотя бы одно уникальное место.");
        }

        Map<String, Boolean> reservedPairs = availabilityService.findReservedPairs(items);
        List<String> validationErrors = validationPolicy.validate(items, context);
        List<ResolvedCartItem> resolvedItems = new ArrayList<>();

        for (BookingCartItemRequestDto item : items) {
            ResolvedPlace resolvedPlace = placeResolver.resolve(item.placeId(), context, coworkingId);
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
                    resolvedPlace.tariff().pricePerDay(),
                    available
            ));
        }

        long totalFinalPrice = resolvedItems.stream().mapToLong(ResolvedCartItem::finalPrice).sum();
        int unavailableCount = (int) resolvedItems.stream().filter(item -> !item.available()).count();
        long currentBalance = unitsService.getBalanceMinorUnits(membership.getId());
        long balanceAfter = currentBalance - totalFinalPrice;
        boolean hasEnoughBalance = balanceAfter >= 0;
        boolean canCheckout = validationErrors.isEmpty() && unavailableCount == 0 && hasEnoughBalance;

        CartCalculationResponseDto response = new CartCalculationResponseDto(
                resolvedItems.stream().map(item -> new CartCalculationResponseDto.CartCalculatedItemDto(
                        item.placeId(),
                        item.placeName(),
                        item.date(),
                        item.floorName(),
                        item.typeName(),
                        item.finalPrice(),
                        item.available()
                )).toList(),
                new CartCalculationResponseDto.CartSummaryDto(
                        totalFinalPrice,
                        unavailableCount,
                        validationErrors,
                        hasEnoughBalance,
                        balanceAfter,
                        canCheckout
                )
        );

        return new CalculatedCart(response, resolvedItems);
    }

}