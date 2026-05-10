package com.hse.userservice.service.booking;

import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.booking.BookingStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingEntityFactory {
    public Booking fromCartItem(Long coworkingId, Long membershipId, String requestId, ResolvedCartItem item) {
        Booking booking = new Booking();
        booking.setCoworkingId(coworkingId);
        booking.setPlaceId(item.placeId());
        booking.setMembershipId(membershipId);
        booking.setDate(item.date());
        booking.setCost(item.finalPrice());
        booking.setActive(true);
        booking.setStatus(BookingStatus.ACTUAL);
        booking.setRequestId(requestId);
        booking.setTariffId(item.tariff().id());
        booking.setPricePerDay(item.tariff().pricePerDay());
        booking.setAppliedDiscountPercent(item.discountPercent());
        booking.setFullRefundHoursBefore(item.tariff().fullRefundHoursBefore());
        booking.setLateCancellationRefundPercent(item.tariff().lateCancellationRefundPercent());
        booking.setCancellationCompensationCoefficient(item.tariff().cancellationCompensationCoefficient());
        booking.setDayClosureCompensationCoefficient(item.tariff().dayClosureCompensationCoefficient());
        booking.setMembershipBlockCompensationCoefficient(item.tariff().membershipBlockCompensationCoefficient());
        return booking;
    }
}
