package com.hse.adminservice.rbac.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingListItemResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessibleCoworkingQueryService {
    private final AccessRepository accessRepository;
    private final CoworkingRepository coworkingRepository;

    public List<CoworkingListItemResponse> getAccessibleCoworkings(Long adminId) {
        Map<Long, CoworkingListItemResponse> result = new LinkedHashMap<>();
        coworkingRepository.findAllByOwnerIdAndArchivedFalse(adminId).forEach(coworking -> result.put(
                coworking.getId(),
                CoworkingListItemResponse.builder()
                        .id(coworking.getId())
                        .name(coworking.getName())
                        .role("Owner")
                        .active(coworking.getActive())
                        .archived(coworking.getArchived())
                        .build()
        ));

        accessRepository.findAllByAdminIdAndActiveTrueAndCoworkingArchivedFalse(adminId)
                .forEach(access -> result.putIfAbsent(
                        access.getCoworking().getId(), CoworkingListItemResponse.builder().id(access.getCoworking()
                                .getId()).name(access.getCoworking().getName()).role(access.getRole().getName()).active(
                                access.getCoworking().getActive()).archived(access.getCoworking().getArchived()).build()
                ));

        return new ArrayList<>(result.values());
    }

    public String resolveDisplayAccessLabel(Long adminId, Coworking coworking) {
        if (Objects.equals(coworking.getOwnerId(), adminId)) {
            return "Owner";
        }

        return accessRepository.findByAdminIdAndCoworkingId(adminId, coworking.getId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()) && !Boolean.TRUE.equals(found.getCoworking()
                        .getArchived()))
                .map(found -> found.getRole().getName())
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }
}
