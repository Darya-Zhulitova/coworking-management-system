package com.hse.adminservice.configlookup.application;

import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.configlookup.dto.BookingContextResponse;
import com.hse.adminservice.configlookup.dto.PlaceSummaryResponse;
import com.hse.adminservice.configlookup.dto.ServiceRequestTypeLookupResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.files.FileStorageService;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.servicecatalog.domain.ServiceRequestType;
import com.hse.adminservice.servicecatalog.persistence.ServiceRequestTypeRepository;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.mapper.PlaceMapper;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingConfigurationLookupService {
    private final CoworkingRepository coworkingRepository;
    private final FloorRepository floorRepository;
    private final TariffRepository tariffRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final PlaceRepository placeRepository;
    private final CoworkingScheduleExceptionRepository scheduleExceptionRepository;
    private final PlaceClosingRepository placeClosingRepository;
    private final ServiceRequestTypeRepository serviceRequestTypeRepository;
    private final PlaceMapper placeMapper;
    private final TimeProvider timeProvider;
    private final FileStorageService fileStorageService;

    public BookingContextResponse getBookingContext(Long coworkingId) {
        var coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
        return BookingContextResponse.builder()
                .coworkingId(coworking.getId())
                .configVersion(coworking.getConfigurationVersion())
                .generatedAt(timeProvider.now())
                .schedule(coworking.getSchedule())
                .name(coworking.getName())
                .floorMapEnabled(Boolean.TRUE.equals(coworking.getFloorMapEnabled()))
                .floors(floorRepository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworkingId)
                        .stream()
                        .map(floor -> BookingContextResponse.FloorDto.builder()
                                .id(floor.getId())
                                .name(floor.getName())
                                .index(floor.getIndex())
                                .imageFileId(floor.getFullImageFileId() != null ? floor.getFullImageFileId() : floor.getImageFileId())
                                .imageUrl(fileStorageService.presignedUrl(floor.getFullImageFileId() != null ? floor.getFullImageFileId() : floor.getImageFileId()))
                                .active(floor.getActive())
                                .build())
                        .toList())
                .tariffs(tariffRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(tariff -> BookingContextResponse.TariffDto.builder()
                                .id(tariff.getId())
                                .name(tariff.getName())
                                .pricePerDay(tariff.getPricePerDay())
                                .fullRefundHoursBefore(tariff.getFullRefundHoursBefore())
                                .lateCancellationRefundPercent(tariff.getLateCancellationRefundPercent())
                                .cancellationCompensationCoefficient(tariff.getCancellationCompensationCoefficient())
                                .dayClosureCompensationCoefficient(tariff.getDayClosureCompensationCoefficient())
                                .membershipBlockCompensationCoefficient(tariff.getMembershipBlockCompensationCoefficient())
                                .version(tariff.getVersion())
                                .active(tariff.getActive())
                                .build())
                        .toList())
                .placeTypes(placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(placeType -> BookingContextResponse.PlaceTypeDto.builder()
                                .id(placeType.getId())
                                .name(placeType.getName())
                                .tariffId(placeType.getTariff().getId())
                                .active(placeType.getActive())
                                .build())
                        .toList())
                .places(placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                        .stream()
                        .map(place -> BookingContextResponse.PlaceDto.builder()
                                .id(place.getId())
                                .name(place.getName())
                                .floorId(place.getFloor().getId())
                                .placeTypeId(place.getPlaceType().getId())
                                .locX(place.getLocX())
                                .locY(place.getLocY())
                                .imageFileId(place.getFullImageFileId() != null ? place.getFullImageFileId() : place.getImageFileId())
                                .previewImageUrl(fileStorageService.presignedUrl(place.getPreviewImageFileId() != null ? place.getPreviewImageFileId() : (place.getFullImageFileId() != null ? place.getFullImageFileId() : place.getImageFileId())))
                                .fullImageUrl(fileStorageService.presignedUrl(place.getFullImageFileId() != null ? place.getFullImageFileId() : place.getImageFileId()))
                                .amenities(placeMapper.parseAmenities(place.getAmenitiesRaw()))
                                .active(place.getActive())
                                .build())
                        .toList())
                .scheduleExceptions(scheduleExceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(
                                coworkingId)
                        .stream()
                        .map(item -> BookingContextResponse.ScheduleExceptionDto.builder()
                                .id(item.getId())
                                .date(item.getDate())
                                .type(item.getType().name())
                                .name(item.getName())
                                .active(item.getActive())
                                .build())
                        .toList())
                .placeClosings(placeClosingRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                        .stream()
                        .map(item -> BookingContextResponse.PlaceClosingDto.builder()
                                .id(item.getId())
                                .placeId(item.getPlace().getId())
                                .date(item.getDate())
                                .name(item.getName())
                                .active(item.getActive())
                                .build())
                        .toList())
                .build();
    }

    public List<ServiceRequestTypeLookupResponse> getServiceRequestTypes(Long coworkingId) {
        ensureCoworkingExists(coworkingId);
        return serviceRequestTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .map(this::toServiceRequestTypeLookup)
                .toList();
    }

    public ServiceRequestTypeLookupResponse getServiceRequestType(Long coworkingId, Long typeId) {
        ServiceRequestType type = serviceRequestTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(
                typeId,
                coworkingId
        ).orElseThrow(() -> new ResourceNotFoundException("Тип сервисной заявки не найден"));
        return toServiceRequestTypeLookup(type);
    }

    public List<PlaceSummaryResponse> getPlaceSummaries(Long coworkingId, Collection<Long> ids) {
        ensureCoworkingExists(coworkingId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> requestedIds = ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }
        return placeRepository.findAllByCoworkingIdAndIdInAndArchivedFalseOrderByNameAsc(coworkingId, requestedIds)
                .stream()
                .map(place -> PlaceSummaryResponse.builder()
                        .id(place.getId())
                        .name(place.getName())
                        .floorName(place.getFloor().getName())
                        .placeTypeName(place.getPlaceType().getName())
                        .previewImageUrl(fileStorageService.presignedUrl(place.getPreviewImageFileId() != null ? place.getPreviewImageFileId() : (place.getFullImageFileId() != null ? place.getFullImageFileId() : place.getImageFileId())))
                        .fullImageUrl(fileStorageService.presignedUrl(place.getFullImageFileId() != null ? place.getFullImageFileId() : place.getImageFileId()))
                        .build())
                .toList();
    }

    private ServiceRequestTypeLookupResponse toServiceRequestTypeLookup(ServiceRequestType item) {
        return ServiceRequestTypeLookupResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .cost(item.getCost())
                .version(item.getVersion())
                .active(item.getActive())
                .build();
    }

    private void ensureCoworkingExists(Long coworkingId) {
        coworkingRepository.findByIdAndArchivedFalse(coworkingId).orElseThrow(() -> new ResourceNotFoundException(
                "Коворкинг не найден"));
    }
}
