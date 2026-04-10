package com.hse.adminservice.service;

import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.dto.CoworkingListItemResponse;
import com.hse.adminservice.dto.CoworkingRoleResponse;
import com.hse.adminservice.dto.CoworkingAccessResponse;
import com.hse.adminservice.entity.*;
import com.hse.adminservice.exception.ConflictException;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.mapper.CoworkingAccessMapper;
import com.hse.adminservice.repository.AccessRepository;
import com.hse.adminservice.repository.AdminRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.RoleRepository;
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
        coworkingRepository.findAllByOwnerIdAndArchivedFalse(adminId).forEach(coworking ->
                result.put(coworking.getId(), CoworkingListItemResponse.builder()
                        .id(coworking.getId())
                        .name(coworking.getName())
                        .role("Owner")
                        .active(coworking.getActive())
                        .archived(coworking.getArchived())
                        .build()));

        accessRepository.findAllByAdminIdAndActiveTrueAndCoworkingArchivedFalse(adminId).forEach(access ->
                result.putIfAbsent(access.getCoworking().getId(), CoworkingListItemResponse.builder()
                        .id(access.getCoworking().getId())
                        .name(access.getCoworking().getName())
                        .role(access.getRole().getName())
                        .active(access.getCoworking().getActive())
                        .archived(access.getCoworking().getArchived())
                        .build()));

        return new ArrayList<>(result.values());
    }


    public String resolveDisplayAccessLabel(Long adminId, Coworking coworking) {
        if (Objects.equals(coworking.getOwnerId(), adminId)) {
            return "Owner";
        }

        return accessRepository.findByAdminIdAndCoworkingId(adminId, coworking.getId())
                .filter(found -> Boolean.TRUE.equals(found.getActive()) && !Boolean.TRUE.equals(found.getCoworking().getArchived()))
                .map(found -> found.getRole().getName())
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }

    public List<CoworkingAccessResponse> getCoworkingAccessList(Long coworkingId) {
        return accessRepository.findAllByCoworkingIdAndCoworkingArchivedFalse(coworkingId).stream()
                .map(this::toStaffResponse)
                .toList();
    }

    public List<CoworkingRoleResponse> getAvailableRoles(Long coworkingId) {
        return roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworkingId).stream()
                .map(this::toRoleResponse)
                .toList();
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
    public CoworkingAccessResponse updateRole(Long coworkingId, Long accessId, Long roleId, Boolean active) {
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

        Set<Grant> grants = roleDefinitions.parseGrants(access.getRole().getGrantsRaw());
        EnumSet<Grant> actions = EnumSet.of(Grant.COWORKING_LIST);
        if (grants.contains(Grant.COWORKING_VIEW)) actions.add(Grant.COWORKING_VIEW);
        if (grants.contains(Grant.COWORKING_EDIT)) actions.add(Grant.COWORKING_EDIT);
        if (grants.contains(Grant.COWORKING_ARCHIVE)) actions.add(Grant.COWORKING_ARCHIVE);
        if (grants.contains(Grant.COWORKING_DASHBOARD_VIEW)) actions.add(Grant.COWORKING_DASHBOARD_VIEW);
        if (grants.contains(Grant.ACCESS_VIEW)) actions.add(Grant.ACCESS_VIEW);
        if (grants.contains(Grant.ACCESS_MANAGE)) actions.add(Grant.ACCESS_MANAGE);
        if (grants.contains(Grant.ROLE_VIEW)) actions.add(Grant.ROLE_VIEW);
        if (grants.contains(Grant.ROLE_ASSIGN)) actions.add(Grant.ROLE_ASSIGN);
        if (grants.contains(Grant.PLACE_VIEW)) actions.add(Grant.PLACE_VIEW);
        if (grants.contains(Grant.PLACE_MANAGE)) actions.add(Grant.PLACE_MANAGE);
        return actions;
    }

    public Set<Grant> resolveOwnerGrantedActions() {
        return EnumSet.of(
                Grant.COWORKING_LIST,
                Grant.COWORKING_VIEW,
                Grant.COWORKING_EDIT,
                Grant.COWORKING_ARCHIVE,
                Grant.COWORKING_DASHBOARD_VIEW,
                Grant.ACCESS_VIEW,
                Grant.ACCESS_MANAGE,
                Grant.ROLE_VIEW,
                Grant.ROLE_ASSIGN,
                Grant.PLACE_VIEW,
                Grant.PLACE_MANAGE
        );
    }

    private CoworkingAccessResponse toStaffResponse(Access access) {
        return coworkingAccessMapper.toResponse(access, resolveGrantedActions(access));
    }

    private CoworkingRoleResponse toRoleResponse(Role role) {
        Access syntheticAccess = Access.builder()
                .role(role)
                .active(true)
                .build();
        return coworkingAccessMapper.toRoleResponse(role, resolveGrantedActions(syntheticAccess));
    }
}
