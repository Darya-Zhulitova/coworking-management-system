package com.hse.adminservice.rbac.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingRoleService {
    private final CoworkingRepository coworkingRepository;
    private final RoleRepository roleRepository;
    private final SystemCoworkingRoleDefinitions roleDefinitions;
    private final CoworkingAccessMapper coworkingAccessMapper;
    private final GrantResolver grantResolver;
    private final TimeProvider timeProvider;

    public List<CoworkingRoleResponse> getAllRoles(Long coworkingId) {
        return roleRepository.findAllByCoworkingIdOrderByNameAsc(coworkingId)
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<CoworkingRoleResponse> getAvailableRoles(Long coworkingId) {
        return roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworkingId)
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public CoworkingRoleResponse getRole(Long coworkingId, Long roleId) {
        Role role = roleRepository.findByIdAndCoworkingId(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));
        return toRoleResponse(role);
    }

    @Transactional
    public CoworkingRoleResponse createRole(Long coworkingId, String name, Set<Grant> grants, Boolean active) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
        if (roleRepository.existsByCoworkingIdAndNameIgnoreCase(coworkingId, name.trim())) {
            throw new ConflictException("Роль с таким названием уже существует в этом коворкинге");
        }
        LocalDateTime now = timeProvider.now();
        Role role = roleRepository.save(Role.builder()
                .coworking(coworking)
                .name(name.trim())
                .grantsRaw(roleDefinitions.serialize(grantResolver.normalizeRoleGrants(grants)))
                .active(Boolean.FALSE.equals(active) ? Boolean.FALSE : Boolean.TRUE)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toRoleResponse(role);
    }

    @Transactional
    public CoworkingRoleResponse updateRoleDefinition(
            Long coworkingId,
            Long roleId,
            String name,
            Set<Grant> grants,
            Boolean active
    ) {
        Role role = roleRepository.findByIdAndCoworkingId(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));
        if (roleRepository.existsByCoworkingIdAndNameIgnoreCaseAndIdNot(coworkingId, name.trim(), roleId)) {
            throw new ConflictException("Роль с таким названием уже существует в этом коворкинге");
        }
        role.setName(name.trim());
        role.setGrantsRaw(roleDefinitions.serialize(grantResolver.normalizeRoleGrants(grants)));
        role.setActive(active);
        role.setUpdatedAt(timeProvider.now());
        return toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public void archiveRole(Long coworkingId, Long roleId) {
        Role role = roleRepository.findByIdAndCoworkingId(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));
        role.setActive(false);
        role.setUpdatedAt(timeProvider.now());
        roleRepository.save(role);
    }

    private CoworkingRoleResponse toRoleResponse(Role role) {
        Access syntheticAccess = Access.builder().role(role).active(true).build();
        return coworkingAccessMapper.toRoleResponse(role, grantResolver.resolveGrantedActions(syntheticAccess));
    }
}
