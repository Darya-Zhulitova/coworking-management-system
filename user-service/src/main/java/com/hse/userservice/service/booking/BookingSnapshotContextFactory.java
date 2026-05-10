package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BookingSnapshotContextFactory {
    public static String key(Long placeId, Object date) {
        return placeId + "#" + date;
    }

    public BookingSnapshotContext from(CoworkingConfigSnapshot snapshot) {
        return new BookingSnapshotContext(
                snapshot.floors().stream().collect(Collectors.toMap(CoworkingConfigSnapshot.Floor::id, item -> item)),
                snapshot.placeTypes()
                        .stream()
                        .collect(Collectors.toMap(CoworkingConfigSnapshot.PlaceType::id, item -> item)),
                snapshot.tariffs().stream().collect(Collectors.toMap(CoworkingConfigSnapshot.Tariff::id, item -> item)),
                snapshot.places().stream().collect(Collectors.toMap(CoworkingConfigSnapshot.Place::id, item -> item)),
                snapshot.scheduleExceptions()
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.active()))
                        .collect(Collectors.toMap(
                                CoworkingConfigSnapshot.ScheduleException::date,
                                item -> item,
                                (first, second) -> second
                        )),
                snapshot.placeClosings()
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.active()))
                        .collect(Collectors.toMap(
                                item -> key(item.placeId(), item.date()),
                                item -> item,
                                (first, second) -> second
                        )),
                snapshot.schedule() == null ? 0 : snapshot.schedule()
        );
    }
}
