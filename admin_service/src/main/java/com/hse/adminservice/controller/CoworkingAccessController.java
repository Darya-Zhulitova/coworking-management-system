package com.hse.adminservice.controller;

import com.hse.adminservice.dto.AssignCoworkingRoleRequest;
import com.hse.adminservice.dto.CoworkingRoleResponse;
import com.hse.adminservice.dto.CoworkingAccessResponse;
import com.hse.adminservice.dto.UpdateCoworkingRoleRequest;
import com.hse.adminservice.service.CoworkingAccessService;
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
        return coworkingStaffService.getAvailableRoles(coworkingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkingAccessResponse assignRole(@PathVariable Long coworkingId, @Valid @RequestBody AssignCoworkingRoleRequest request) {
        return coworkingStaffService.assignRole(coworkingId, request);
    }

    @PutMapping("/{accessId}")
    public CoworkingAccessResponse updateRole(
            @PathVariable Long coworkingId,
            @PathVariable Long accessId,
            @Valid @RequestBody UpdateCoworkingRoleRequest request
    ) {
        return coworkingStaffService.updateRole(coworkingId, accessId, request);
    }

    @DeleteMapping("/{accessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long coworkingId, @PathVariable Long accessId) {
        coworkingStaffService.deactivate(coworkingId, accessId);
    }
}
