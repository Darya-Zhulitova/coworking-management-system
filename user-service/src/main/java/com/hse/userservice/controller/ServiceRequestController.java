package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CreateServiceRequestDto;
import com.hse.userservice.dto.response.ServiceRequestDto;
import com.hse.userservice.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestDto create(@Valid @RequestBody CreateServiceRequestDto dto) {
        return serviceRequestService.create(dto);
    }

    @GetMapping
    public List<ServiceRequestDto> getAll() {
        return serviceRequestService.getAll();
    }

    @GetMapping("/membership/{membershipId}")
    public List<ServiceRequestDto> getByMembershipId(@PathVariable Long membershipId) {
        return serviceRequestService.getByMembershipId(membershipId);
    }
}