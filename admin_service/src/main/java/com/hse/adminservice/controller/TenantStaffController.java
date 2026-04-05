package com.hse.adminservice.controller;

import com.hse.adminservice.dto.AssignTenantRoleRequest;
import com.hse.adminservice.dto.TenantRoleResponse;
import com.hse.adminservice.dto.TenantStaffMemberResponse;
import com.hse.adminservice.dto.UpdateTenantRoleRequest;
import com.hse.adminservice.service.TenantStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/staff")
@RequiredArgsConstructor
public class TenantStaffController {

    private final TenantStaffService tenantStaffService;

    @GetMapping
    public List<TenantStaffMemberResponse> getStaff(@PathVariable Long coworkingId) {
        return tenantStaffService.getStaff(coworkingId);
    }

    @GetMapping("/roles")
    public List<TenantRoleResponse> getRoles(@PathVariable Long coworkingId) {
        return tenantStaffService.getAvailableRoles(coworkingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantStaffMemberResponse assignRole(@PathVariable Long coworkingId, @Valid @RequestBody AssignTenantRoleRequest request) {
        return tenantStaffService.assignRole(coworkingId, request);
    }

    @PutMapping("/{accessId}")
    public TenantStaffMemberResponse updateRole(
            @PathVariable Long coworkingId,
            @PathVariable Long accessId,
            @Valid @RequestBody UpdateTenantRoleRequest request
    ) {
        return tenantStaffService.updateRole(coworkingId, accessId, request);
    }

    @DeleteMapping("/{accessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long coworkingId, @PathVariable Long accessId) {
        tenantStaffService.deactivate(coworkingId, accessId);
    }
}
