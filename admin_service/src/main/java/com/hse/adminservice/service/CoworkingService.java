package com.hse.adminservice.service;

import com.hse.adminservice.dto.CoworkingCreateRequest;
import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.dto.CoworkingUpdateRequest;

import java.util.List;

public interface CoworkingService {

    CoworkingResponse create(CoworkingCreateRequest request);

    List<CoworkingResponse> getAll();

    CoworkingResponse getById(Long id);

    CoworkingResponse update(Long id, CoworkingUpdateRequest request);

    void archive(Long id);
}