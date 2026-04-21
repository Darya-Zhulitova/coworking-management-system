package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CreateMembershipDto;
import com.hse.userservice.dto.response.CoworkingDetailsDto;
import com.hse.userservice.dto.response.MembershipDto;
import com.hse.userservice.dto.response.UserMembershipSummaryDto;
import com.hse.userservice.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService membershipService;

    @PostMapping("/api/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipDto create(@Valid @RequestBody CreateMembershipDto dto) {
        return membershipService.create(dto);
    }

    @GetMapping("/api/users/me/memberships")
    public List<UserMembershipSummaryDto> getCurrentUserMemberships() {
        return membershipService.getCurrentUserMemberships();
    }

    @GetMapping("/api/memberships/{id}")
    public MembershipDto getById(@PathVariable Long id) {
        return membershipService.getById(id);
    }

    @GetMapping("/api/coworkings/{coworkingId}")
    public CoworkingDetailsDto getCoworking(@PathVariable Long coworkingId) {
        return membershipService.getCoworkingDetails(coworkingId);
    }
}
