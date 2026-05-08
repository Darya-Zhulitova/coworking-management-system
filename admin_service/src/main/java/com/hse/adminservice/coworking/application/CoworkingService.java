package com.hse.adminservice.coworking.application;

import com.hse.adminservice.coworking.dto.CoworkingCreateRequest;
import com.hse.adminservice.coworking.dto.CoworkingDashboardResponse;
import com.hse.adminservice.coworking.dto.CoworkingResponse;
import com.hse.adminservice.coworking.dto.CoworkingUpdateRequest;

import java.util.List;

public interface CoworkingService {
    CoworkingResponse create(CoworkingCreateRequest request);

    List<CoworkingResponse> getAll();

    CoworkingResponse getById(Long id);

    CoworkingResponse update(Long id, CoworkingUpdateRequest request);

    void archive(Long id);

    CoworkingDashboardResponse getDashboard(Long id);

    List<CoworkingResponse> getArchived();
}