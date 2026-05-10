package com.hse.adminservice.configsnapshot.application;

import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.configsnapshot.dto.CoworkingConfigSnapshotResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.files.FileStorageService;
import com.hse.adminservice.pricing.discount.domain.TariffDiscountRule;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.servicecatalog.persistence.ServiceRequestTypeRepository;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.mapper.PlaceMapper;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingConfigSnapshotService {
    private final CoworkingRepository coworkingRepository;
    private final FloorRepository floorRepository;
    private final TariffRepository tariffRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final PlaceRepository placeRepository;
    private final CoworkingScheduleExceptionRepository scheduleExceptionRepository;
    private final PlaceClosingRepository placeClosingRepository;
    private final ServiceRequestTypeRepository serviceRequestTypeRepository;
    private final PlaceMapper placeMapper;
    private final com.hse.adminservice.coworking.mapper.CoworkingMapper coworkingMapper;
    private final TimeProvider timeProvider;
    private final FileStorageService fileStorageService;

    public CoworkingConfigSnapshotResponse getSnapshot(Long coworkingId) {
        var coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        return CoworkingConfigSnapshotResponse.builder()
                .coworkingId(coworking.getId())
                .configVersion(coworking.getConfigurationVersion())
                .generatedAt(timeProvider.now())
                .schedule(coworking.getSchedule())
                .name(coworking.getName())
                .description(coworking.getDescription())
                .address(coworking.getAddress())
                .workingHoursLabel(coworking.getWorkingHoursLabel())
                .heroTitle(coworking.getHeroTitle())
                .heroText(coworking.getHeroText())
                .imageUrls(coworkingMapper.readImageUrls(coworking.getImageUrlsJson()))
                .floors(floorRepository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworkingId)
                        .stream()
                        .map(floor -> CoworkingConfigSnapshotResponse.SnapshotFloorDto.builder()
                                .id(floor.getId())
                                .name(floor.getName())
                                .index(floor.getIndex())
                                .imageFileId(floor.getImageFileId())
                                .imageUrl(fileStorageService.presignedUrl(floor.getImageFileId()))
                                .active(floor.getActive())
                                .build())
                        .toList())
                .tariffs(tariffRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(tariff -> CoworkingConfigSnapshotResponse.SnapshotTariffDto.builder()
                                .id(tariff.getId())
                                .name(tariff.getName())
                                .pricePerDay(tariff.getPricePerDay())
                                .minBookingDays(tariff.getMinBookingDays())
                                .fullRefundHoursBefore(tariff.getFullRefundHoursBefore())
                                .lateCancellationRefundPercent(tariff.getLateCancellationRefundPercent())
                                .cancellationCompensationCoefficient(tariff.getCancellationCompensationCoefficient())
                                .dayClosureCompensationCoefficient(tariff.getDayClosureCompensationCoefficient())
                                .membershipBlockCompensationCoefficient(tariff.getMembershipBlockCompensationCoefficient())
                                .discountRules(tariff.getDiscountRules()
                                        .stream()
                                        .sorted(Comparator.comparing(TariffDiscountRule::getThresholdQuantity))
                                        .map(rule -> CoworkingConfigSnapshotResponse.SnapshotTariffDiscountRuleDto.builder()
                                                .id(rule.getId())
                                                .thresholdQuantity(rule.getThresholdQuantity())
                                                .discountPercent(rule.getDiscountPercent())
                                                .build())
                                        .toList())
                                .version(tariff.getVersion())
                                .active(tariff.getActive())
                                .build())
                        .toList())
                .placeTypes(placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(placeType -> CoworkingConfigSnapshotResponse.SnapshotPlaceTypeDto.builder()
                                .id(placeType.getId())
                                .name(placeType.getName())
                                .tariffId(placeType.getTariff().getId())
                                .active(placeType.getActive())
                                .build())
                        .toList())
                .places(placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(place -> CoworkingConfigSnapshotResponse.SnapshotPlaceDto.builder()
                                .id(place.getId())
                                .name(place.getName())
                                .floorId(place.getFloor().getId())
                                .placeTypeId(place.getPlaceType()
                                        .getId())
                                .locX(place.getLocX())
                                .locY(place.getLocY())
                                .amenities(placeMapper.parseAmenities(place.getAmenitiesRaw()))
                                .active(place.getActive())
                                .build())
                        .toList())
                .scheduleExceptions(scheduleExceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(
                                coworkingId)
                        .stream()
                        .map(item -> CoworkingConfigSnapshotResponse.SnapshotScheduleExceptionDto.builder()
                                .id(item.getId())
                                .date(item.getDate())
                                .type(item.getType().name())
                                .name(item.getName())
                                .active(item.getActive())
                                .build())
                        .toList())
                .placeClosings(placeClosingRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                        .stream()
                        .map(item -> CoworkingConfigSnapshotResponse.SnapshotPlaceClosingDto.builder()
                                .id(item.getId())
                                .placeId(item.getPlace().getId())
                                .date(item.getDate())
                                .name(item.getName())
                                .active(item.getActive())
                                .build())
                        .toList())
                .serviceRequestTypes(serviceRequestTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(
                                coworkingId)
                        .stream()
                        .map(item -> CoworkingConfigSnapshotResponse.SnapshotServiceRequestTypeDto.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .cost(item.getCost())
                                .version(item.getVersion())
                                .active(item.getActive())
                                .build())
                        .toList())
                .build();
    }
}
