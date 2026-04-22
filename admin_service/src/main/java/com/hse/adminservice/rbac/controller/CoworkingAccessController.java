package com.hse.adminservice.rbac.controller;

import com.hse.adminservice.rbac.dto.*;
import com.hse.adminservice.rbac.service.CoworkingAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/staff")
@RequiredArgsConstructor
public class CoworkingAccessController {
    private final CoworkingAccessService coworkingStaffService;

    @GetMapping
    public List<CoworkingAccessResponse> getStaff(@PathVariable Long coworkingId) {
        return coworkingStaffService.getStaff(coworkingId);
    }

    @GetMapping("/roles")
    public List<CoworkingRoleResponse> getRoles(@PathVariable Long coworkingId) {
        return coworkingStaffService.getAllRoles(coworkingId);
    }

    @GetMapping("/roles/{roleId}")
    public CoworkingRoleResponse getRole(@PathVariable Long coworkingId, @PathVariable Long roleId) {
        return coworkingStaffService.getRole(coworkingId, roleId);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkingRoleResponse createRole(
            @PathVariable Long coworkingId,
            @Valid @RequestBody RoleCreateRequest request
    ) {
        return coworkingStaffService.createRole(coworkingId, request);
    }

    @PutMapping("/roles/{roleId}")
    public CoworkingRoleResponse updateRole(
            @PathVariable Long coworkingId,
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        return coworkingStaffService.updateRole(coworkingId, roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveRole(@PathVariable Long coworkingId, @PathVariable Long roleId) {
        coworkingStaffService.archiveRole(coworkingId, roleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkingAccessResponse assignRole(
            @PathVariable Long coworkingId,
            @Valid @RequestBody AssignCoworkingRoleRequest request
    ) {
        return coworkingStaffService.assignRole(coworkingId, request);
    }

    @PutMapping("/{accessId}")
    public CoworkingAccessResponse updateAccessRole(
            @PathVariable Long coworkingId,
            @PathVariable Long accessId,
            @Valid @RequestBody UpdateCoworkingRoleRequest request
    ) {
        return coworkingStaffService.updateAssignedRole(coworkingId, accessId, request);
    }

    @DeleteMapping("/{accessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long coworkingId, @PathVariable Long accessId) {
        coworkingStaffService.deactivate(coworkingId, accessId);
    }
}
