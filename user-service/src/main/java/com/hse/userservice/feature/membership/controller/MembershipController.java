package com.hse.userservice.feature.membership.controller;

import com.hse.userservice.feature.membership.dto.*;
import com.hse.userservice.feature.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService membershipService;

    @GetMapping("/api/coworkings/join/{joinToken}")
    public JoinCoworkingPreviewDto getJoinPreview(@PathVariable String joinToken) {
        return membershipService.getJoinPreview(joinToken);
    }

    @PostMapping("/api/coworkings/join/{joinToken}")
    public JoinCoworkingResultDto joinCoworking(@PathVariable String joinToken) {
        return membershipService.joinByToken(joinToken);
    }

    @GetMapping("/api/users/me/memberships")
    public List<UserMembershipSummaryDto> getCurrentUserMemberships() {
        return membershipService.getCurrentUserMemberships();
    }

    @GetMapping("/api/memberships/{membershipId}")
    public CoworkingDetailsDto getMembershipCoworking(@PathVariable Long membershipId) {
        return membershipService.getMembershipCoworkingDetails(membershipId);
    }

    @GetMapping("/api/memberships/{membershipId}/context")
    public CoworkingContextDto getMembershipContext(@PathVariable Long membershipId) {
        return membershipService.getMembershipContext(membershipId);
    }
}
