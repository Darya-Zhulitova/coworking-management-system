package com.hse.adminservice.servicecatalog.application;

import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeCreateRequest;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeResponse;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeUpdateRequest;

import java.util.List;

public interface ServiceRequestTypeService {
    ServiceRequestTypeResponse create(Long coworkingId, ServiceRequestTypeCreateRequest request);

    List<ServiceRequestTypeResponse> getAll(Long coworkingId);

    ServiceRequestTypeResponse getById(Long coworkingId, Long serviceRequestTypeId);

    ServiceRequestTypeResponse update(
            Long coworkingId,
            Long serviceRequestTypeId,
            ServiceRequestTypeUpdateRequest request
    );

    void archive(Long coworkingId, Long serviceRequestTypeId);
}
