package com.hse.adminservice.coworking.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingJoinLinkResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingJoinLinkService {
    private final CoworkingRepository coworkingRepository;
    private final AdminAuthorizationService authorizationService;
    private final TimeProvider timeProvider;

    @Value("${integration.user-frontend.join-base-url:http://localhost:3001/join}")
    private String joinBaseUrl;

    public CoworkingJoinLinkResponse getJoinLink(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.COWORKING_READ);
        Coworking coworking = requireCoworking(coworkingId);
        return toResponse(coworking);
    }

    @Transactional
    public CoworkingJoinLinkResponse generateJoinLink(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.COWORKING_EDIT);
        Coworking coworking = requireCoworking(coworkingId);
        coworking.setJoinToken(generateUniqueToken());
        coworking.setUpdatedAt(timeProvider.now());
        return toResponse(coworkingRepository.save(coworking));
    }

    @Transactional
    public CoworkingJoinLinkResponse deleteJoinLink(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.COWORKING_EDIT);
        Coworking coworking = requireCoworking(coworkingId);
        coworking.setJoinToken(null);
        coworking.setUpdatedAt(timeProvider.now());
        return toResponse(coworkingRepository.save(coworking));
    }

    private Coworking requireCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (coworkingRepository.existsByJoinToken(token));
        return token;
    }

    private CoworkingJoinLinkResponse toResponse(Coworking coworking) {
        String token = coworking.getJoinToken();
        return CoworkingJoinLinkResponse.builder()
                .coworkingId(coworking.getId())
                .joinToken(token)
                .joinUrl(token == null ? null : buildJoinUrl(token))
                .build();
    }

    private String buildJoinUrl(String token) {
        return joinBaseUrl.replaceAll("/+$", "") + "/" + token;
    }
}
