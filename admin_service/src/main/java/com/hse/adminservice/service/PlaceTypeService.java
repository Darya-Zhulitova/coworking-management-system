package com.hse.adminservice.service;

import com.hse.adminservice.dto.PlaceTypeCreateRequest;
import com.hse.adminservice.dto.PlaceTypeResponse;
import com.hse.adminservice.dto.PlaceTypeUpdateRequest;

import java.util.List;

public interface PlaceTypeService {
    PlaceTypeResponse create(Long coworkingId, PlaceTypeCreateRequest request);
    List<PlaceTypeResponse> getAll(Long coworkingId);
    PlaceTypeResponse getById(Long coworkingId, Long placeTypeId);
    PlaceTypeResponse update(Long coworkingId, Long placeTypeId, PlaceTypeUpdateRequest request);
    void archive(Long coworkingId, Long placeTypeId);
}
