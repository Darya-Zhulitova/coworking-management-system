package com.hse.adminservice.service;

import com.hse.adminservice.dto.AccessibleCoworkingResponse;
import com.hse.adminservice.entity.AdminCoworkingAccess;
import com.hse.adminservice.entity.AdminCoworkingRole;
import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.repository.AdminCoworkingAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hse.adminservice.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCoworkingAccessService {

    private final AdminCoworkingAccessRepository accessRepository;

    public List<AccessibleCoworkingResponse> getAccessibleCoworkings(Long adminUserId) {
        return accessRepository.findAllByAdminUserIdAndActiveTrueAndCoworkingArchivedFalse(adminUserId).stream().map(
                access -> AccessibleCoworkingResponse.builder()
                        .id(access.getCoworking().getId())
                        .name(access.getCoworking().getName())
                        .role(access.getRole())
                        .build()).toList();
    }

    public void requireAccess(Long adminUserId, Long coworkingId) {
        boolean hasAccess = accessRepository.existsByAdminUserIdAndCoworkingIdAndActiveTrueAndCoworkingArchivedFalse(
                adminUserId,
                coworkingId
        );
        if (!hasAccess) {
            throw new ResourceNotFoundException("Coworking not found");
        }
    }

    public AdminCoworkingAccess grantOwnerAccess(AdminUser adminUser, Coworking coworking) {
        return accessRepository.findByAdminUserIdAndCoworkingId(adminUser.getId(), coworking.getId())
                .orElseGet(() -> accessRepository.save(AdminCoworkingAccess.builder()
                        .adminUser(adminUser)
                        .coworking(coworking)
                        .role(AdminCoworkingRole.OWNER)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }
}
