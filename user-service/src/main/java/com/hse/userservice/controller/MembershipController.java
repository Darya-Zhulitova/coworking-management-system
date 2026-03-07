package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CreateMembershipDto;
import com.hse.userservice.dto.response.MembershipDto;
import com.hse.userservice.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipDto create(@Valid @RequestBody CreateMembershipDto dto) {
        return membershipService.create(dto);
    }

    @GetMapping
    public List<MembershipDto> getAll() {
        return membershipService.getAll();
    }

    @GetMapping("/{id}")
    public MembershipDto getById(@PathVariable Long id) {
        return membershipService.getById(id);
    }
}