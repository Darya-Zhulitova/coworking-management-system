package com.hse.adminservice.coworking.service;

import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.coworking.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoworkingConfigurationVersionService {
    private final CoworkingRepository coworkingRepository;

    @Transactional
    public void bumpVersion(Long coworkingId) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        long nextVersion = (coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L;
        coworking.setConfigurationVersion(nextVersion);
        coworking.setUpdatedAt(LocalDateTime.now());
        coworkingRepository.save(coworking);
    }
}
