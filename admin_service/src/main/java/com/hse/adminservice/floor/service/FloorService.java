package com.hse.adminservice.floor.service;

import com.hse.adminservice.floor.dto.FloorCreateRequest;
import com.hse.adminservice.floor.dto.FloorResponse;
import com.hse.adminservice.floor.dto.FloorUpdateRequest;

import java.util.List;

public interface FloorService {
    FloorResponse create(Long coworkingId, FloorCreateRequest request);

    List<FloorResponse> getAll(Long coworkingId);

    FloorResponse getById(Long coworkingId, Long floorId);

    FloorResponse update(Long coworkingId, Long floorId, FloorUpdateRequest request);

    void archive(Long coworkingId, Long floorId);
}
