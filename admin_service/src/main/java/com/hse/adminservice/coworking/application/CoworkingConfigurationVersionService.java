package com.hse.adminservice.coworking.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CoworkingConfigurationVersionService {
    private final CoworkingRepository coworkingRepository;
    private final TimeProvider timeProvider;

    @Transactional
    public void bumpVersion(Long coworkingId) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalseForUpdate(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));

        long nextVersion = (coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L;
        coworking.setConfigurationVersion(nextVersion);
        coworking.setUpdatedAt(timeProvider.now());
        coworkingRepository.save(coworking);
    }
}
