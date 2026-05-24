package com.hse.userservice.support.config;

import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestAdminServiceClientConfig {
    @Bean
    @Primary
    public FakeAdminServiceClient fakeAdminServiceClient() {
        return new FakeAdminServiceClient();
    }

    public static class FakeAdminServiceClient implements AdminServiceClient {
        private final Map<Long, CoworkingInfo> coworkings = new HashMap<>();
        private final Map<String, Long> joinTokens = new HashMap<>();
        private final Map<Long, BookingContext> bookingContexts = new HashMap<>();
        private final Map<Long, List<ServiceRequestTypeInfo>> serviceRequestTypes = new HashMap<>();
        private final Map<Long, List<PlaceSummary>> placeSummaries = new HashMap<>();

        public FakeAdminServiceClient() {
            reset();
        }

        public void reset() {
            coworkings.clear();
            joinTokens.clear();
            bookingContexts.clear();
            serviceRequestTypes.clear();
            placeSummaries.clear();

            CoworkingInfo coworking = new CoworkingInfo(
                    1L,
                    "Test Coworking",
                    "Coworking for integration tests",
                    "Test address",
                    "09:00-21:00",
                    "Welcome",
                    "Book a workplace in tests",
                    List.of("https://example.test/coworking.jpg"),
                    true,
                    true,
                    true
            );
            coworkings.put(coworking.id(), coworking);
            joinTokens.put("test-join-token", coworking.id());

            BookingContext context = new BookingContext(
                    coworking.id(),
                    1L,
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    127,
                    coworking.name(),
                    true,
                    List.of(new BookingContext.Floor(1L, "Floor 1", 1, null, null, true)),
                    List.of(new BookingContext.Tariff(
                            1L,
                            "Daily",
                            1000L,
                            24,
                            50,
                            new BigDecimal("1.0000"),
                            new BigDecimal("1.0000"),
                            new BigDecimal("1.0000"),
                            1,
                            true
                    )),
                    List.of(new BookingContext.PlaceType(1L, "Desk", 1L, true)),
                    List.of(new BookingContext.Place(
                            1L,
                            "Desk 1",
                            1L,
                            1L,
                            new BigDecimal("0.5000"),
                            new BigDecimal("0.5000"),
                            null,
                            null,
                            null,
                            List.of("socket", "wifi"),
                            true
                    )),
                    List.of(),
                    List.of()
            );
            bookingContexts.put(coworking.id(), context);
            serviceRequestTypes.put(coworking.id(), List.of(new ServiceRequestTypeInfo(1L, "Water", 0L, 1, true)));
            placeSummaries.put(coworking.id(), List.of(new PlaceSummary(1L, "Desk 1", "Floor 1", "Desk", null, null)));
        }

        public void putCoworking(CoworkingInfo coworkingInfo) {
            coworkings.put(coworkingInfo.id(), coworkingInfo);
        }

        public void putJoinToken(String token, Long coworkingId) {
            joinTokens.put(token, coworkingId);
        }

        public void putBookingContext(BookingContext bookingContext) {
            bookingContexts.put(bookingContext.coworkingId(), bookingContext);
        }

        public void putServiceRequestTypes(Long coworkingId, List<ServiceRequestTypeInfo> types) {
            serviceRequestTypes.put(coworkingId, List.copyOf(types));
        }

        public void putPlaceSummaries(Long coworkingId, List<PlaceSummary> summaries) {
            placeSummaries.put(coworkingId, List.copyOf(summaries));
        }

        @Override
        public CoworkingInfo getCoworkingInfo(Long coworkingId) {
            CoworkingInfo coworkingInfo = coworkings.get(coworkingId);
            if (coworkingInfo == null) {
                throw new ResourceNotFoundException("Коворкинг не найден: " + coworkingId);
            }
            return coworkingInfo;
        }

        @Override
        public CoworkingInfo getCoworkingInfoByJoinToken(String joinToken) {
            Long coworkingId = joinTokens.get(joinToken);
            if (coworkingId == null) {
                throw new ResourceNotFoundException("Ссылка приглашения в коворкинг не найдена: " + joinToken);
            }
            return getCoworkingInfo(coworkingId);
        }

        @Override
        public BookingContext getBookingContext(Long coworkingId) {
            BookingContext context = bookingContexts.get(coworkingId);
            if (context == null) {
                throw new ResourceNotFoundException("Booking context not found: " + coworkingId);
            }
            return context;
        }

        @Override
        public List<ServiceRequestTypeInfo> getServiceRequestTypes(Long coworkingId) {
            return serviceRequestTypes.getOrDefault(coworkingId, List.of());
        }

        @Override
        public ServiceRequestTypeInfo getServiceRequestType(Long coworkingId, Long typeId) {
            return getServiceRequestTypes(coworkingId).stream()
                    .filter(type -> type.id().equals(typeId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Тип сервисной заявки не найден: " + typeId));
        }

        @Override
        public List<PlaceSummary> getPlaceSummaries(Long coworkingId, Collection<Long> placeIds) {
            if (placeIds == null || placeIds.isEmpty()) {
                return List.of();
            }
            return placeSummaries.getOrDefault(coworkingId, List.of()).stream()
                    .filter(summary -> placeIds.contains(summary.id()))
                    .toList();
        }
    }
}
