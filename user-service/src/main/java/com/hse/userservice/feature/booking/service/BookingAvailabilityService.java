package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.integration.dto.BookingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityService {
    private static final int MONDAY_BIT = 1;
    private static final int TUESDAY_BIT = 1 << 1;
    private static final int WEDNESDAY_BIT = 1 << 2;
    private static final int THURSDAY_BIT = 1 << 3;
    private static final int FRIDAY_BIT = 1 << 4;
    private static final int SATURDAY_BIT = 1 << 5;
    private static final int SUNDAY_BIT = 1 << 6;

    private final BookingRepository bookingRepository;

    public Map<String, Boolean> findReservedPairs(List<BookingCartItemRequestDto> items) {
        Set<Long> placeIds = items.stream().map(BookingCartItemRequestDto::placeId).collect(Collectors.toSet());
        Set<LocalDate> dates = items.stream().map(BookingCartItemRequestDto::date).collect(Collectors.toSet());
        if (placeIds.isEmpty() || dates.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(placeIds, dates)
                .stream()
                .collect(Collectors.toMap(
                        item -> BookingSnapshotContextFactory.key(item.getPlaceId(), item.getDate()),
                        item -> Boolean.TRUE,
                        (first, second) -> first
                ));
    }

    public Map<String, Boolean> findReservedPairsForDate(Collection<Long> placeIds, LocalDate date) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(placeIds, Set.of(date)).stream().collect(
                Collectors.toMap(
                        item -> BookingSnapshotContextFactory.key(item.getPlaceId(), item.getDate()),
                        item -> Boolean.TRUE,
                        (first, second) -> first
                ));
    }

    public Map<String, Boolean> findReservedPairsForDates(Collection<Long> placeIds, Collection<LocalDate> dates) {
        if (placeIds.isEmpty() || dates.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(placeIds, dates)
                .stream()
                .collect(Collectors.toMap(
                        item -> BookingSnapshotContextFactory.key(item.getPlaceId(), item.getDate()),
                        item -> Boolean.TRUE,
                        (first, second) -> first
                ));
    }

    public boolean isPlaceAvailable(
            LocalDate date,
            BookingContext.Place place,
            BookingSnapshotContext context,
            Map<String, Boolean> reservedPairs
    ) {
        if (!Boolean.TRUE.equals(place.active())) {
            return false;
        }
        if (context.placeClosingsByPlaceAndDate().containsKey(BookingSnapshotContextFactory.key(place.id(), date))) {
            return false;
        }
        BookingContext.ScheduleException scheduleException = context.scheduleExceptionsByDate().get(date);
        if (scheduleException != null) {
            if ("CLOSE".equalsIgnoreCase(scheduleException.type())) {
                return false;
            }
            if ("OPEN".equalsIgnoreCase(scheduleException.type())) {
                return !reservedPairs.getOrDefault(BookingSnapshotContextFactory.key(place.id(), date), Boolean.FALSE);
            }
        }
        if (!isDateEnabledBySchedule(date, context.scheduleBitmask())) {
            return false;
        }
        return !reservedPairs.getOrDefault(BookingSnapshotContextFactory.key(place.id(), date), Boolean.FALSE);
    }

    private boolean isDateEnabledBySchedule(LocalDate date, int schedule) {
        int bit = switch (date.getDayOfWeek()) {
            case MONDAY -> MONDAY_BIT;
            case TUESDAY -> TUESDAY_BIT;
            case WEDNESDAY -> WEDNESDAY_BIT;
            case THURSDAY -> THURSDAY_BIT;
            case FRIDAY -> FRIDAY_BIT;
            case SATURDAY -> SATURDAY_BIT;
            case SUNDAY -> SUNDAY_BIT;
        };
        return (schedule & bit) != 0;
    }
}
