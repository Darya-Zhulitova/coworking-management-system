package com.hse.adminservice.coworking.service;

import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.coworking.dto.CoworkingPublicInfoResponse;
import com.hse.adminservice.coworking.mapper.CoworkingMapper;
import com.hse.adminservice.coworking.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingPublicInfoService {
    private final CoworkingRepository coworkingRepository;
    private final CoworkingMapper coworkingMapper;

    public CoworkingPublicInfoResponse getById(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .map(coworkingMapper::toPublicInfoResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }
}
