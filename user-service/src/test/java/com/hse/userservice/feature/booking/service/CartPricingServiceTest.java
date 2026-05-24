package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartPricingServiceTest {
    private final UnitsService unitsService = mock(UnitsService.class);
    private final BookingSnapshotContextFactory contextFactory = new BookingSnapshotContextFactory();
    private final BookingPlaceResolver placeResolver = new BookingPlaceResolver();
    private final BookingAvailabilityService availabilityService = mock(BookingAvailabilityService.class);
    private final CartValidationPolicy validationPolicy = spy(new CartValidationPolicy());
    private final CartItemNormalizer normalizer = new CartItemNormalizer();
    private final CartPricingService service = new CartPricingService(
            unitsService,
            contextFactory,
            placeResolver,
            availabilityService,
            validationPolicy,
            normalizer
    );

    @Test
    void calculateBuildsCartSummaryFromUniqueItemsPriceAvailabilityAndBalance() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 1L);
        BookingContext context = BookingServiceUnitFixtures.bookingContext();
        LocalDate date = BookingServiceUnitFixtures.TODAY.plusDays(1);
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(5_000L);
        when(availabilityService.findReservedPairs(any())).thenReturn(Map.of());
        when(availabilityService.isPlaceAvailable(eq(date), any(), any(), any())).thenReturn(true);

        CalculatedCart cart = service.calculate(
                1L,
                membership,
                context,
                List.of(new BookingCartItemRequestDto(100L, date), new BookingCartItemRequestDto(100L, date))
        );

        assertThat(cart.resolvedItems()).hasSize(1);
        assertThat(cart.resolvedItems().getFirst().placeName()).isEqualTo("Place 100");
        assertThat(cart.resolvedItems().getFirst().finalPrice()).isEqualTo(1_500L);
        assertThat(cart.response().items()).hasSize(1);
        assertThat(cart.response().summary().totalFinalPrice()).isEqualTo(1_500L);
        assertThat(cart.response().summary().unavailableCount()).isZero();
        assertThat(cart.response().summary().hasEnoughBalance()).isTrue();
        assertThat(cart.response().summary().balanceAfterMinorUnits()).isEqualTo(3_500L);
        assertThat(cart.response().summary().canCheckout()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingCartItemRequestDto>> itemsCaptor = ArgumentCaptor.forClass((Class<List<BookingCartItemRequestDto>>) (Class<?>) List.class);
        verify(availabilityService).findReservedPairs(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
    }

    @Test
    void calculateMarksCheckoutForbiddenWhenPlaceIsUnavailableOrBalanceIsInsufficient() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 1L);
        LocalDate date = BookingServiceUnitFixtures.TODAY.plusDays(1);
        when(unitsService.getBalanceMinorUnits(11L)).thenReturn(1_000L);
        when(availabilityService.findReservedPairs(any())).thenReturn(Map.of(BookingSnapshotContextFactory.key(100L, date), true));
        when(availabilityService.isPlaceAvailable(eq(date), any(), any(), any())).thenReturn(false);

        CalculatedCart cart = service.calculate(
                1L,
                membership,
                BookingServiceUnitFixtures.bookingContext(),
                List.of(new BookingCartItemRequestDto(100L, date))
        );

        assertThat(cart.response().summary().totalFinalPrice()).isEqualTo(1_500L);
        assertThat(cart.response().summary().unavailableCount()).isEqualTo(1);
        assertThat(cart.response().summary().hasEnoughBalance()).isFalse();
        assertThat(cart.response().summary().balanceAfterMinorUnits()).isEqualTo(-500L);
        assertThat(cart.response().summary().canCheckout()).isFalse();
        assertThat(cart.response().items().getFirst().available()).isFalse();
    }

    @Test
    void calculateRejectsEmptyCartAfterNormalization() {
        Membership membership = BookingServiceUnitFixtures.activeMembership(11L, 1L);

        assertThatThrownBy(() -> service.calculate(1L, membership, BookingServiceUnitFixtures.bookingContext(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Корзина должна содержать");

        verifyNoInteractions(availabilityService, unitsService);
    }
}
