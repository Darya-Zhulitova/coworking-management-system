package com.hse.adminservice.coworking.controller;

import com.hse.adminservice.coworking.dto.CoworkingPublicInfoResponse;
import com.hse.adminservice.coworking.service.CoworkingPublicInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/coworkings")
@RequiredArgsConstructor
public class CoworkingPublicInfoController {
    private final CoworkingPublicInfoService coworkingPublicInfoService;

    @GetMapping("/{coworkingId}/info")
    public CoworkingPublicInfoResponse getPublicInfo(@PathVariable Long coworkingId) {
        return coworkingPublicInfoService.getById(coworkingId);
    }
}
