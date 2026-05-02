package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CreateServiceRequestDto;
import com.hse.userservice.dto.request.CreateServiceRequestMessageDto;
import com.hse.userservice.dto.response.ServiceRequestDto;
import com.hse.userservice.dto.response.ServiceRequestMessageDto;
import com.hse.userservice.dto.response.ServiceRequestTypeOptionDto;
import com.hse.userservice.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;

    @GetMapping("/api/coworkings/{coworkingId}/service-requests")
    public List<ServiceRequestDto> getByCoworkingId(@PathVariable Long coworkingId) {
        return serviceRequestService.getByCoworkingId(coworkingId);
    }

    @GetMapping("/api/coworkings/{coworkingId}/service-request-types")
    public List<ServiceRequestTypeOptionDto> getTypes(@PathVariable Long coworkingId) {
        return serviceRequestService.getTypes(coworkingId);
    }

    @GetMapping("/api/coworkings/{coworkingId}/service-requests/{requestId}")
    public ServiceRequestDto getDetails(@PathVariable Long coworkingId, @PathVariable Long requestId) {
        return serviceRequestService.getDetails(coworkingId, requestId);
    }

    @GetMapping("/api/coworkings/{coworkingId}/service-requests/{requestId}/messages")
    public List<ServiceRequestMessageDto> getMessages(@PathVariable Long coworkingId, @PathVariable Long requestId) {
        return serviceRequestService.getMessages(coworkingId, requestId);
    }

    @PostMapping("/api/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestDto create(@Valid @RequestBody CreateServiceRequestDto dto) {
        return serviceRequestService.create(dto);
    }

    @PostMapping("/api/service-requests/{requestId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestMessageDto addMessage(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateServiceRequestMessageDto dto
    ) {
        return serviceRequestService.addMessage(requestId, dto);
    }
}
