package com.hse.adminservice.coworking.api;

import com.hse.adminservice.coworking.application.CoworkingJoinLinkService;
import com.hse.adminservice.coworking.dto.CoworkingJoinLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/join-link")
@RequiredArgsConstructor
public class CoworkingJoinLinkController {
    private final CoworkingJoinLinkService joinLinkService;

    @GetMapping
    public CoworkingJoinLinkResponse getJoinLink(@PathVariable Long coworkingId) {
        return joinLinkService.getJoinLink(coworkingId);
    }

    @PostMapping
    public CoworkingJoinLinkResponse generateJoinLink(@PathVariable Long coworkingId) {
        return joinLinkService.generateJoinLink(coworkingId);
    }

    @DeleteMapping
    public CoworkingJoinLinkResponse deleteJoinLink(@PathVariable Long coworkingId) {
        return joinLinkService.deleteJoinLink(coworkingId);
    }
}
