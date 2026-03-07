package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingInfo;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.dto.request.CreateMembershipDto;
import com.hse.userservice.dto.response.MembershipDto;
import com.hse.userservice.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final AdminServiceClient adminServiceClient;

    public MembershipDto create(CreateMembershipDto dto) {
        CoworkingInfo coworking = adminServiceClient.getCoworkingById(dto.coworkingId());

        if (coworking == null) {
            throw new IllegalArgumentException("Coworking not found: " + dto.coworkingId());
        }

        if (!coworking.active()) {
            throw new IllegalArgumentException("Coworking is inactive: " + dto.coworkingId());
        }

        Membership membership = new Membership();
        membership.setCoworkingId(dto.coworkingId());

        Membership saved = membershipRepository.save(membership);
        return toDto(saved);
    }

    public List<MembershipDto> getAll() {
        return membershipRepository.findAll().stream().map(this::toDto).toList();
    }

    public MembershipDto getById(Long id) {
        Membership membership = membershipRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membership not found: " + id));

        return toDto(membership);
    }

    private MembershipDto toDto(Membership membership) {
        return new MembershipDto(membership.getId(), membership.getCoworkingId(), membership.getBalance(), membership.getStatus(), membership.getCreatedAt());
    }
}