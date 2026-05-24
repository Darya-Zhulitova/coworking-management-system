package com.hse.adminservice.coworking.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.dto.CoworkingPublicInfoResponse;
import com.hse.adminservice.coworking.mapper.CoworkingMapper;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
    }

    public CoworkingPublicInfoResponse getByJoinToken(String joinToken) {
        if (joinToken == null || joinToken.isBlank()) {
            throw new ResourceNotFoundException("Ссылка приглашения в коворкинг не найдена");
        }
        return coworkingRepository.findByJoinTokenAndArchivedFalse(joinToken.trim())
                .map(coworkingMapper::toPublicInfoResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ссылка приглашения в коворкинг не найдена"));
    }
}
