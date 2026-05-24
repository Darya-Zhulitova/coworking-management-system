package com.hse.userservice.feature.servicerequest.controller;

import com.hse.userservice.feature.servicerequest.dto.CreateServiceRequestDto;
import com.hse.userservice.feature.servicerequest.dto.ServiceRequestDto;
import com.hse.userservice.feature.servicerequest.dto.ServiceRequestMessageDto;
import com.hse.userservice.feature.servicerequest.dto.ServiceRequestTypeOptionDto;
import com.hse.userservice.feature.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberships/{membershipId}/service-requests")
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;

    @GetMapping
    public List<ServiceRequestDto> getByMembershipId(@PathVariable Long membershipId) {
        return serviceRequestService.getByMembershipId(membershipId);
    }

    @GetMapping("/types")
    public List<ServiceRequestTypeOptionDto> getTypes(@PathVariable Long membershipId) {
        return serviceRequestService.getTypes(membershipId);
    }

    @GetMapping("/{requestId}")
    public ServiceRequestDto getDetails(@PathVariable Long membershipId, @PathVariable Long requestId) {
        return serviceRequestService.getDetails(membershipId, requestId);
    }

    @GetMapping("/{requestId}/messages")
    public List<ServiceRequestMessageDto> getMessages(@PathVariable Long membershipId, @PathVariable Long requestId) {
        return serviceRequestService.getMessages(membershipId, requestId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestDto create(@PathVariable Long membershipId, @Valid @RequestBody CreateServiceRequestDto dto) {
        return serviceRequestService.create(membershipId, dto);
    }

    @PostMapping(value = "/{requestId}/messages", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestMessageDto addMessage(
            @PathVariable Long membershipId,
            @PathVariable Long requestId,
            @RequestParam(required = false) String text,
            @RequestPart(required = false) MultipartFile file
    ) {
        return serviceRequestService.addMessage(membershipId, requestId, text, file);
    }
}
