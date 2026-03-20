package com.hse.adminservice.service;

import com.hse.adminservice.dto.PlaceCreateRequest;
import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.dto.PlaceUpdateRequest;

import java.util.List;

public interface PlaceService {

    PlaceResponse create(Long coworkingId, PlaceCreateRequest request);

    List<PlaceResponse> getAll(Long coworkingId);

    PlaceResponse getById(Long coworkingId, Long placeId);

    PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request);

    void archive(Long coworkingId, Long placeId);
}
