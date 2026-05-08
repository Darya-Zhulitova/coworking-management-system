package com.hse.adminservice.space.placetype.application;

import com.hse.adminservice.space.placetype.dto.PlaceTypeCreateRequest;
import com.hse.adminservice.space.placetype.dto.PlaceTypeResponse;
import com.hse.adminservice.space.placetype.dto.PlaceTypeUpdateRequest;

import java.util.List;

public interface PlaceTypeService {
    PlaceTypeResponse create(Long coworkingId, PlaceTypeCreateRequest request);

    List<PlaceTypeResponse> getAll(Long coworkingId);

    PlaceTypeResponse getById(Long coworkingId, Long placeTypeId);

    PlaceTypeResponse update(Long coworkingId, Long placeTypeId, PlaceTypeUpdateRequest request);

    void archive(Long coworkingId, Long placeTypeId);
}
