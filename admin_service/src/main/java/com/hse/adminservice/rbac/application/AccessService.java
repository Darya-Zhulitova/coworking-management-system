package com.hse.adminservice.rbac.application;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingListItemResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {

    private final AccessRepository accessRepository;
    private final AdminRepository adminRepository;
    private final CoworkingRepository coworkingRepository;
    private final RoleRepository roleRepository;
    private final SystemCoworkingRoleDefinitions roleDefinitions;
    private final CoworkingAccessMapper coworkingAccessMapper;

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

    public List<CoworkingAccessResponse> getCoworkingAccessList(Long coworkingId) {
        return accessRepository.findAllByCoworkingIdAndCoworkingArchivedFalse(coworkingId)
                .stream()
                .map(this::toStaffResponse)
                .toList();
    }

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
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return toRoleResponse(role);
    }

    @Transactional
    public CoworkingRoleResponse createRole(Long coworkingId, String name, Set<Grant> grants, Boolean active) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        if (roleRepository.existsByCoworkingIdAndNameIgnoreCase(coworkingId, name.trim())) {
            throw new ConflictException("Role name already exists in this coworking");
        }
        LocalDateTime now = LocalDateTime.now();
        Role role = roleRepository.save(Role.builder()
                .coworking(coworking)
                .name(name.trim())
                .grantsRaw(roleDefinitions.serialize(normalizeRoleGrants(grants)))
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
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (roleRepository.existsByCoworkingIdAndNameIgnoreCaseAndIdNot(coworkingId, name.trim(), roleId)) {
            throw new ConflictException("Role name already exists in this coworking");
        }
        role.setName(name.trim());
        role.setGrantsRaw(roleDefinitions.serialize(normalizeRoleGrants(grants)));
        role.setActive(active);
        role.setUpdatedAt(LocalDateTime.now());
        return toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public void archiveRole(Long coworkingId, Long roleId) {
        Role role = roleRepository.findByIdAndCoworkingId(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        role.setActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    @Transactional
    public CoworkingAccessResponse assignRole(Long coworkingId, String email, Long roleId) {
        Admin admin = adminRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        Role role = roleRepository.findByIdAndCoworkingIdAndActiveTrue(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (Objects.equals(coworking.getOwnerId(), admin.getId())) {
            throw new ConflictException("Owner is not managed through staff access");
        }

        if (accessRepository.findByAdminIdAndCoworkingId(admin.getId(), coworkingId).isPresent()) {
            throw new ConflictException("Admin already has access to this coworking");
        }

        Access access = accessRepository.save(Access.builder()
                .admin(admin)
                .coworking(coworking)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(access.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));
    }

    @Transactional
    public CoworkingAccessResponse updateAccessRole(Long coworkingId, Long accessId, Long roleId, Boolean active) {
        Access access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));

        Role role = roleRepository.findByIdAndCoworkingIdAndActiveTrue(roleId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        access.setRole(role);
        access.setActive(active);
        access.setUpdatedAt(LocalDateTime.now());
        Access saved = accessRepository.save(access);

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(saved.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));
    }

    @Transactional
    public void deactivate(Long coworkingId, Long accessId) {
        Access access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));

        access.setActive(false);
        access.setUpdatedAt(LocalDateTime.now());
        accessRepository.save(access);
    }

    public Set<Grant> resolveGrantedActions(Access access) {
        if (!Boolean.TRUE.equals(access.getActive()) || !Boolean.TRUE.equals(access.getRole().getActive())) {
            return EnumSet.noneOf(Grant.class);
        }
        return normalizeRoleGrants(roleDefinitions.parseGrants(access.getRole().getGrantsRaw()));
    }

    public Set<Grant> resolveOwnerGrantedActions() {
        return EnumSet.allOf(Grant.class);
    }

    private EnumSet<Grant> normalizeRoleGrants(Set<Grant> grants) {
        EnumSet<Grant> normalized = grants == null || grants.isEmpty() ? EnumSet.noneOf(Grant.class) : EnumSet.copyOf(
                grants);
        if (normalized.contains(Grant.COWORKING_EDIT))
            normalized.add(Grant.COWORKING_READ);
        if (normalized.contains(Grant.FLOOR_EDIT))
            normalized.add(Grant.FLOOR_READ);
        if (normalized.contains(Grant.PLACE_TYPE_EDIT))
            normalized.add(Grant.PLACE_TYPE_READ);
        if (normalized.contains(Grant.PLACE_EDIT))
            normalized.add(Grant.PLACE_READ);
        if (normalized.contains(Grant.TARIFF_EDIT))
            normalized.add(Grant.TARIFF_READ);
        if (normalized.contains(Grant.SERVICE_REQUEST_TYPE_EDIT))
            normalized.add(Grant.SERVICE_REQUEST_TYPE_READ);
        if (normalized.contains(Grant.ROLE_EDIT))
            normalized.add(Grant.ROLE_READ);
        if (normalized.contains(Grant.ACCESS_EDIT))
            normalized.add(Grant.ACCESS_READ);
        if (normalized.contains(Grant.SCHEDULE_EDIT))
            normalized.add(Grant.SCHEDULE_READ);
        if (normalized.contains(Grant.USER_EDIT))
            normalized.add(Grant.USER_READ);
        if (normalized.contains(Grant.BOOKING_EDIT))
            normalized.add(Grant.BOOKING_READ);
        return normalized;
    }

    private CoworkingAccessResponse toStaffResponse(Access access) {
        return coworkingAccessMapper.toResponse(access, resolveGrantedActions(access));
    }

    private CoworkingRoleResponse toRoleResponse(Role role) {
        Access syntheticAccess = Access.builder().role(role).active(true).build();
        return coworkingAccessMapper.toRoleResponse(role, resolveGrantedActions(syntheticAccess));
    }
}
