package com.hse.adminservice.rbac.application;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingStaffAccessService {
    private final AccessRepository accessRepository;
    private final AdminRepository adminRepository;
    private final CoworkingRepository coworkingRepository;
    private final RoleRepository roleRepository;
    private final CoworkingAccessMapper coworkingAccessMapper;
    private final GrantResolver grantResolver;
    private final TimeProvider timeProvider;

    public List<CoworkingAccessResponse> getCoworkingAccessList(Long coworkingId) {
        return accessRepository.findAllByCoworkingIdAndCoworkingArchivedFalse(coworkingId)
                .stream()
                .map(this::toStaffResponse)
                .toList();
    }

    @Transactional
    public CoworkingAccessResponse assignRole(Long coworkingId, String email, Long roleId) {
        Admin admin = adminRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Администратор не найден"));

        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));

        Role role = roleRepository.findByIdAndCoworkingIdAndActiveTrue(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));

        if (Objects.equals(coworking.getOwnerId(), admin.getId())) {
            throw new ConflictException("Владелец не управляется через доступ сотрудников");
        }

        if (accessRepository.findByAdminIdAndCoworkingId(admin.getId(), coworkingId).isPresent()) {
            throw new ConflictException("У администратора уже есть доступ к этому коворкингу");
        }

        LocalDateTime now = timeProvider.now();
        Access access = accessRepository.save(Access.builder()
                .admin(admin)
                .coworking(coworking)
                .role(role)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(access.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Доступ сотрудника не найден"));
    }

    @Transactional
    public CoworkingAccessResponse updateAccessRole(Long coworkingId, Long accessId, Long roleId, Boolean active) {
        Access access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Доступ сотрудника не найден"));

        Role role = roleRepository.findByIdAndCoworkingIdAndActiveTrue(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));

        access.setRole(role);
        access.setActive(active);
        access.setUpdatedAt(timeProvider.now());
        Access saved = accessRepository.save(access);

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(saved.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Доступ сотрудника не найден"));
    }

    @Transactional
    public void deactivate(Long coworkingId, Long accessId) {
        Access access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Доступ сотрудника не найден"));

        access.setActive(false);
        access.setUpdatedAt(timeProvider.now());
        accessRepository.save(access);
    }

    private CoworkingAccessResponse toStaffResponse(Access access) {
        return coworkingAccessMapper.toResponse(access, grantResolver.resolveGrantedActions(access));
    }
}
