package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.booking.BookingStatus;
import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.ledger.LedgerEntryType;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.dto.request.BookingCartItemRequestDto;
import com.hse.userservice.dto.request.CartCalculateRequestDto;
import com.hse.userservice.dto.request.CreateFromCartRequestDto;
import com.hse.userservice.dto.response.*;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.exception.ResourceNotFoundException;
import com.hse.userservice.repository.BookingRepository;
import com.hse.userservice.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final int MONDAY_BIT = 1;
    private static final int TUESDAY_BIT = 1 << 1;
    private static final int WEDNESDAY_BIT = 1 << 2;
    private static final int THURSDAY_BIT = 1 << 3;
    private static final int FRIDAY_BIT = 1 << 4;
    private static final int SATURDAY_BIT = 1 << 5;
    private static final int SUNDAY_BIT = 1 << 6;

    private final MembershipService membershipService;
    private final AdminServiceClient adminServiceClient;
    private final BalanceService balanceService;
    private final BookingRepository bookingRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public BookingInitResponseDto getBookingInit(Long coworkingId, LocalDate previewDate) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        SnapshotContext context = buildSnapshotContext(snapshot);
        LocalDate effectivePreviewDate = previewDate == null ? LocalDate.now() : previewDate;

        Map<String, Boolean> reservedPairs = findReservedPairsForDate(
                snapshot.places()
                        .stream()
                        .map(CoworkingConfigSnapshot.Place::id)
                        .collect(Collectors.toSet()),
                effectivePreviewDate
        );
        List<BookingInitResponseDto.PlaceItemDto> availablePlaces = snapshot.places().stream().map(place -> toInitPlace(
                place,
                effectivePreviewDate,
                context,
                reservedPairs
        )).filter(place -> Boolean.TRUE.equals(place.previewAvailable())).toList();

        return new BookingInitResponseDto(
                coworkingId,
                snapshot.name(),
                membership.getId(),
                membership.getStatus().name().toLowerCase(),
                balanceService.getBalanceMinorUnits(membership.getId()),
                effectivePreviewDate,
                snapshot.floors()
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.active()))
                        .map(item -> new BookingInitResponseDto.FloorItemDto(item.id(), item.name(), item.index()))
                        .toList(),
                snapshot.placeTypes()
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.active()))
                        .map(item -> new BookingInitResponseDto.PlaceTypeItemDto(
                                item.id(),
                                item.name(),
                                item.tariffId()
                        ))
                        .toList(),
                snapshot.tariffs()
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.active()))
                        .map(item -> new BookingInitResponseDto.TariffItemDto(
                                item.id(),
                                item.name(),
                                item.pricePerDay(),
                                item.minBookingDays(),
                                item.discountRules()
                                        .stream()
                                        .map(rule -> new BookingInitResponseDto.DiscountRuleItemDto(
                                                rule.id(),
                                                rule.thresholdQuantity(),
                                                rule.discountPercent()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                availablePlaces
        );
    }

    @Transactional(readOnly = true)
    public List<BookingListItemDto> getBookings(Long coworkingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        Map<Long, CoworkingConfigSnapshot.Place> placesById = snapshot.places().stream().collect(Collectors.toMap(
                CoworkingConfigSnapshot.Place::id,
                item -> item
        ));

        return bookingRepository.findAllByMembershipIdOrderByDateDesc(membership.getId())
                .stream()
                .map(item -> toBookingListItem(item, placesById))
                .toList();
    }

    @Transactional
    public CancelBookingResponseDto cancelBooking(Long coworkingId, Long bookingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        requireActiveMembership(membership);

        Booking booking = bookingRepository.findByIdAndMembershipId(bookingId, membership.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!Objects.equals(booking.getCoworkingId(), coworkingId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.ACTUAL) {
            throw new ResourceConflictException("Booking is already cancelled.");
        }

        long refundAmount = calculateCancellationPreview(booking);
        if (refundAmount <= 0) {
            throw new ResourceConflictException("This booking can no longer be cancelled.");
        }

        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELED_USER);
        bookingRepository.save(booking);

        LedgerEntry refund = new LedgerEntry();
        refund.setMembershipId(membership.getId());
        refund.setCoworkingId(coworkingId);
        refund.setType(LedgerEntryType.CANCELLATION_REFUND);
        refund.setAmount(refundAmount);
        refund.setName("Возврат по отмене бронирования");
        refund.setComment(buildCancellationComment(booking, refundAmount));
        refund.setTimestamp(LocalDateTime.now());
        ledgerEntryRepository.save(refund);

        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        Map<Long, CoworkingConfigSnapshot.Place> placesById = snapshot.places().stream().collect(Collectors.toMap(
                CoworkingConfigSnapshot.Place::id,
                item -> item
        ));

        return new CancelBookingResponseDto(
                booking.getId(),
                coworkingId,
                refundAmount,
                balanceService.getBalanceMinorUnits(membership.getId()),
                toBookingListItem(booking, placesById)
        );
    }

    @Transactional(readOnly = true)
    public CartCalculationResponseDto calculateCart(CartCalculateRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        requireActiveMembership(membership);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(dto.coworkingId());
        CalculatedCart cart = calculateInternal(dto.coworkingId(), membership, snapshot, dto.items());
        return cart.response();
    }

    @Transactional
    public CreateFromCartResponseDto createFromCart(CreateFromCartRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        requireActiveMembership(membership);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(dto.coworkingId());
        CalculatedCart cart = calculateInternal(dto.coworkingId(), membership, snapshot, dto.items());

        if (!cart.response().summary().canCheckout()) {
            throw new ResourceConflictException(firstCartError(cart.response().summary()));
        }

        String requestId = "REQ-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        List<Booking> bookings = cart.resolvedItems().stream().map(item -> toBookingEntity(
                dto.coworkingId(),
                membership.getId(),
                requestId,
                item
        )).toList();

        try {
            bookingRepository.saveAll(bookings);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("One or more booking slots are no longer available.");
        }

        LedgerEntry charge = new LedgerEntry();
        charge.setMembershipId(membership.getId());
        charge.setCoworkingId(dto.coworkingId());
        charge.setType(LedgerEntryType.BOOKING_CHARGE);
        charge.setAmount(-cart.response().summary().totalFinalPrice());
        charge.setName("Оплата бронирования");
        charge.setComment(buildChargeComment(cart.resolvedItems()));
        charge.setTimestamp(LocalDateTime.now());
        ledgerEntryRepository.save(charge);

        long balanceAfter = balanceService.getBalanceMinorUnits(membership.getId());
        Map<Long, CoworkingConfigSnapshot.Place> placesById = snapshot.places().stream().collect(Collectors.toMap(
                CoworkingConfigSnapshot.Place::id,
                item -> item
        ));

        return new CreateFromCartResponseDto(
                dto.coworkingId(),
                requestId,
                cart.response().summary().totalFinalPrice(),
                balanceAfter,
                bookings.stream().map(item -> toBookingListItem(item, placesById)).toList()
        );
    }

    private CalculatedCart calculateInternal(
            Long coworkingId,
            Membership membership,
            CoworkingConfigSnapshot snapshot,
            List<BookingCartItemRequestDto> rawItems
    ) {
        SnapshotContext context = buildSnapshotContext(snapshot);
        List<BookingCartItemRequestDto> items = normalizeItems(rawItems);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one unique item.");
        }

        Map<String, Boolean> reservedPairs = findReservedPairs(items);
        Map<Long, Integer> quantityByTariffId = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        for (BookingCartItemRequestDto item : items) {
            ResolvedPlace resolvedPlace = resolvePlace(item.placeId(), context, coworkingId);
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
            ResolvedPlace resolvedPlace = resolvePlace(item.placeId(), context, coworkingId);
            int quantity = quantityByTariffId.getOrDefault(resolvedPlace.tariff().id(), 0);
            int discountPercent = resolveDiscountPercent(resolvedPlace.tariff(), quantity);
            long basePrice = resolvedPlace.tariff().pricePerDay();
            long discountAmount = Math.floorDiv(basePrice * discountPercent, 100);
            boolean available = isPlaceAvailable(item.date(), resolvedPlace.place(), context, reservedPairs);

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
                coworkingId,
                resolvedItems.stream()
                        .map(item -> new CartCalculationResponseDto.CartCalculatedItemDto(
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
                        ))
                        .toList(),
                new CartCalculationResponseDto.CartSummaryDto(
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

    private BookingInitResponseDto.PlaceItemDto toInitPlace(
            CoworkingConfigSnapshot.Place place,
            LocalDate previewDate,
            SnapshotContext context,
            Map<String, Boolean> reservedPairs
    ) {
        CoworkingConfigSnapshot.Floor floor = context.floorsById().get(place.floorId());
        CoworkingConfigSnapshot.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        CoworkingConfigSnapshot.Tariff tariff = placeType == null ? null : context.tariffsById()
                .get(placeType.tariffId());
        boolean previewAvailable = tariff != null && isPlaceAvailable(previewDate, place, context, reservedPairs);

        return new BookingInitResponseDto.PlaceItemDto(
                place.id(),
                place.name(),
                place.floorId(),
                floor == null ? "—" : floor.name(),
                place.placeTypeId(),
                placeType == null ? "—" : placeType.name(),
                tariff == null ? null : tariff.id(),
                tariff == null ? 0 : tariff.pricePerDay(),
                place.amenities(),
                place.active(),
                previewAvailable
        );
    }

    private BookingListItemDto toBookingListItem(Booking booking, Map<Long, CoworkingConfigSnapshot.Place> placesById) {
        CoworkingConfigSnapshot.Place place = placesById.get(booking.getPlaceId());
        return new BookingListItemDto(
                booking.getId(),
                booking.getCoworkingId(),
                booking.getPlaceId(),
                place == null ? "Unknown place" : place.name(),
                booking.getDate(),
                booking.getCost(),
                booking.getActive(),
                booking.getStatus().name(),
                booking.getRequestId(),
                booking.getTariffId(),
                booking.getPricePerDay(),
                booking.getAppliedDiscountPercent(),
                booking.getFullRefundHoursBefore(),
                booking.getLateCancellationRefundPercent(),
                booking.getActive() ? calculateCancellationPreview(booking) : 0L
        );
    }

    private Booking toBookingEntity(Long coworkingId, Long membershipId, String requestId, ResolvedCartItem item) {
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

    private SnapshotContext buildSnapshotContext(CoworkingConfigSnapshot snapshot) {
        return new SnapshotContext(
                snapshot.floors()
                        .stream()
                        .collect(Collectors.toMap(CoworkingConfigSnapshot.Floor::id, item -> item)),
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
                                item -> item.placeId() + "#" + item.date(),
                                item -> item,
                                (first, second) -> second
                        )),
                snapshot.schedule() == null ? 0 : snapshot.schedule()
        );
    }

    private ResolvedPlace resolvePlace(Long placeId, SnapshotContext context, Long coworkingId) {
        CoworkingConfigSnapshot.Place place = context.placesById().get(placeId);
        if (place == null) {
            throw new ResourceNotFoundException("Place not found: " + placeId);
        }
        CoworkingConfigSnapshot.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        if (placeType == null) {
            throw new ResourceConflictException("Place type not found for place " + placeId);
        }
        CoworkingConfigSnapshot.Tariff tariff = context.tariffsById().get(placeType.tariffId());
        if (tariff == null) {
            throw new ResourceConflictException("Tariff not found for place " + placeId);
        }
        CoworkingConfigSnapshot.Floor floor = context.floorsById().get(place.floorId());
        if (floor == null) {
            throw new ResourceConflictException("Floor not found for place " + placeId);
        }
        if (!Boolean.TRUE.equals(place.active()) || !Boolean.TRUE.equals(placeType.active()) || !Boolean.TRUE.equals(
                tariff.active())) {
            throw new ResourceConflictException("Place " + placeId + " is not available for booking.");
        }
        return new ResolvedPlace(place, placeType, tariff, floor, coworkingId);
    }

    private List<BookingCartItemRequestDto> normalizeItems(List<BookingCartItemRequestDto> items) {
        LinkedHashMap<String, BookingCartItemRequestDto> unique = new LinkedHashMap<>();
        for (BookingCartItemRequestDto item : items) {
            unique.put(item.placeId() + "#" + item.date(), item);
        }
        return new ArrayList<>(unique.values());
    }

    private Map<String, Boolean> findReservedPairs(List<BookingCartItemRequestDto> items) {
        Set<Long> placeIds = items.stream().map(BookingCartItemRequestDto::placeId).collect(Collectors.toSet());
        Set<LocalDate> dates = items.stream().map(BookingCartItemRequestDto::date).collect(Collectors.toSet());
        if (placeIds.isEmpty() || dates.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(placeIds, dates)
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getPlaceId() + "#" + item.getDate(),
                        item -> Boolean.TRUE,
                        (first, second) -> first
                ));
    }

    private Map<String, Boolean> findReservedPairsForDate(Collection<Long> placeIds, LocalDate date) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(placeIds, Set.of(date)).stream().collect(
                Collectors.toMap(
                        item -> item.getPlaceId() + "#" + item.getDate(),
                        item -> Boolean.TRUE,
                        (first, second) -> first
                ));
    }

    private boolean isPlaceAvailable(
            LocalDate date,
            CoworkingConfigSnapshot.Place place,
            SnapshotContext context,
            Map<String, Boolean> reservedPairs
    ) {
        if (!Boolean.TRUE.equals(place.active())) {
            return false;
        }
        if (context.placeClosingsByPlaceAndDate().containsKey(place.id() + "#" + date)) {
            return false;
        }
        CoworkingConfigSnapshot.ScheduleException scheduleException = context.scheduleExceptionsByDate().get(date);
        if (scheduleException != null) {
            if ("CLOSE".equalsIgnoreCase(scheduleException.type())) {
                return false;
            }
            if ("OPEN".equalsIgnoreCase(scheduleException.type())) {
                return !reservedPairs.getOrDefault(place.id() + "#" + date, Boolean.FALSE);
            }
        }
        if (!isDateEnabledBySchedule(date, context.scheduleBitmask())) {
            return false;
        }
        return !reservedPairs.getOrDefault(place.id() + "#" + date, Boolean.FALSE);
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

    private int resolveDiscountPercent(CoworkingConfigSnapshot.Tariff tariff, int quantity) {
        return tariff.discountRules().stream().filter(rule -> quantity >= rule.thresholdQuantity()).mapToInt(
                CoworkingConfigSnapshot.TariffDiscountRule::discountPercent).max().orElse(0);
    }

    private List<String> buildDiscountHints(Map<Long, Integer> quantityByTariffId, SnapshotContext context) {
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

    private long calculateCancellationPreview(Booking booking) {
        LocalDateTime bookingStart = booking.getDate().atStartOfDay();
        if (!bookingStart.isAfter(LocalDateTime.now())) {
            return 0L;
        }

        long hoursUntil = Duration.between(LocalDateTime.now(), bookingStart).toHours();
        if (hoursUntil >= booking.getFullRefundHoursBefore()) {
            return booking.getCost();
        }
        return Math.floorDiv(booking.getCost() * booking.getLateCancellationRefundPercent(), 100);
    }

    private String buildCancellationComment(Booking booking, long refundAmount) {
        return "Бронирование " + booking.getRequestId() + " · placeId=" + booking.getPlaceId() + " · " + booking.getDate() + " · refund=" + refundAmount;
    }

    private String buildChargeComment(List<ResolvedCartItem> items) {
        return items.stream().map(item -> item.placeName() + " - " + item.date()).collect(Collectors.joining(", "));
    }

    private void requireActiveMembership(Membership membership) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ResourceConflictException("Only active membership can create bookings.");
        }
    }

    private String firstCartError(CartCalculationResponseDto.CartSummaryDto summary) {
        if (!summary.validationErrors().isEmpty()) {
            return summary.validationErrors().getFirst();
        }
        if (summary.unavailableCount() > 0) {
            return "One or more booking slots are no longer available.";
        }
        if (!Boolean.TRUE.equals(summary.hasEnoughBalance())) {
            return "Insufficient balance for checkout.";
        }
        return "Unable to create booking from cart.";
    }

    private record SnapshotContext(
            Map<Long, CoworkingConfigSnapshot.Floor> floorsById,
            Map<Long, CoworkingConfigSnapshot.PlaceType> placeTypesById,
            Map<Long, CoworkingConfigSnapshot.Tariff> tariffsById,
            Map<Long, CoworkingConfigSnapshot.Place> placesById,
            Map<LocalDate, CoworkingConfigSnapshot.ScheduleException> scheduleExceptionsByDate,
            Map<String, CoworkingConfigSnapshot.PlaceClosing> placeClosingsByPlaceAndDate,
            int scheduleBitmask
    ) {
    }

    private record ResolvedPlace(
            CoworkingConfigSnapshot.Place place,
            CoworkingConfigSnapshot.PlaceType placeType,
            CoworkingConfigSnapshot.Tariff tariff,
            CoworkingConfigSnapshot.Floor floor,
            Long coworkingId
    ) {
    }

    private record ResolvedCartItem(
            Long placeId,
            String placeName,
            LocalDate date,
            String floorName,
            String typeName,
            CoworkingConfigSnapshot.Tariff tariff,
            long basePrice,
            int discountPercent,
            long discountAmount,
            long finalPrice,
            boolean available
    ) {
    }

    private record CalculatedCart(
            CartCalculationResponseDto response,
            List<ResolvedCartItem> resolvedItems
    ) {
    }
}
