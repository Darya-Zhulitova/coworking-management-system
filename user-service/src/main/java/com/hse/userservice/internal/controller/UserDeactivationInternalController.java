package com.hse.userservice.internal.controller;

import com.hse.userservice.internal.dto.deactivation.OperationalImpactResponse;
import com.hse.userservice.internal.dto.deactivation.UserDeactivateOperationRequest;
import com.hse.userservice.internal.service.UserDeactivationInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/deactivate")
@RequiredArgsConstructor
public class UserDeactivationInternalController {
    private final UserDeactivationInternalService service;

    @PostMapping("/preview")
    public OperationalImpactResponse preview(@Valid @RequestBody UserDeactivateOperationRequest request) {
        return service.preview(request);
    }

    @PostMapping("/commit")
    public OperationalImpactResponse commit(@Valid @RequestBody UserDeactivateOperationRequest request) {
        return service.commit(request);
    }
}
