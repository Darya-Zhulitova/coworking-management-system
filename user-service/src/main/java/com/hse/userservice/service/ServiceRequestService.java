package com.hse.userservice.service;

import com.hse.userservice.domain.request.ServiceRequest;
import com.hse.userservice.dto.request.CreateServiceRequestDto;
import com.hse.userservice.dto.response.*;
import com.hse.userservice.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestDto create(CreateServiceRequestDto dto) {
        validateCreateRequest(dto);

        ServiceRequest request = new ServiceRequest();
        request.setMembershipId(dto.membershipId());
        request.setPlaceId(dto.placeId());
        request.setBookingId(dto.bookingId());
        request.setCategory(dto.category().trim());
        request.setDescription(dto.description().trim());

        ServiceRequest saved = serviceRequestRepository.save(request);
        return toDto(saved);
    }

    public List<ServiceRequestDto> getAll() {
        return serviceRequestRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<ServiceRequestDto> getByMembershipId(Long membershipId) {
        return serviceRequestRepository.findByMembershipIdOrderByCreatedAtDesc(membershipId).stream().map(this::toDto).toList();
    }

    private void validateCreateRequest(CreateServiceRequestDto dto) {
        if (dto.membershipId() == null) {
            throw new IllegalArgumentException("membershipId must not be null");
        }
        if (dto.category() == null || dto.category().isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (dto.description() == null || dto.description().isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    private ServiceRequestDto toDto(ServiceRequest request) {
        return new ServiceRequestDto(request.getId(), request.getMembershipId(), request.getPlaceId(), request.getBookingId(), request.getCategory(), request.getDescription(), request.getStatus(), request.getCreatedAt());
    }
}