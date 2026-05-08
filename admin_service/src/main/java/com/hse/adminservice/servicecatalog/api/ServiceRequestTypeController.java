package com.hse.adminservice.servicecatalog.api;

import com.hse.adminservice.servicecatalog.application.ServiceRequestTypeService;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeCreateRequest;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeResponse;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/service-request-types")
@RequiredArgsConstructor
public class ServiceRequestTypeController {

    private final ServiceRequestTypeService serviceRequestTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestTypeResponse create(
            @PathVariable Long coworkingId,
            @Valid @RequestBody ServiceRequestTypeCreateRequest request
    ) {
        return serviceRequestTypeService.create(coworkingId, request);
    }

    @GetMapping
    public List<ServiceRequestTypeResponse> getAll(@PathVariable Long coworkingId) {
        return serviceRequestTypeService.getAll(coworkingId);
    }

    @GetMapping("/{serviceRequestTypeId}")
    public ServiceRequestTypeResponse getById(@PathVariable Long coworkingId, @PathVariable Long serviceRequestTypeId) {
        return serviceRequestTypeService.getById(coworkingId, serviceRequestTypeId);
    }

    @PutMapping("/{serviceRequestTypeId}")
    public ServiceRequestTypeResponse update(
            @PathVariable Long coworkingId,
            @PathVariable Long serviceRequestTypeId,
            @Valid @RequestBody ServiceRequestTypeUpdateRequest request
    ) {
        return serviceRequestTypeService.update(coworkingId, serviceRequestTypeId, request);
    }

    @DeleteMapping("/{serviceRequestTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long coworkingId, @PathVariable Long serviceRequestTypeId) {
        serviceRequestTypeService.archive(coworkingId, serviceRequestTypeId);
    }
}
