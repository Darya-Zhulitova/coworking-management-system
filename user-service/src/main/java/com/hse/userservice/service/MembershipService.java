package com.hse.userservice.service;

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

    public MembershipDto create(CreateMembershipDto dto) {
        validateCreateRequest(dto);

        Membership membership = new Membership();
        membership.setCoworkingId(dto.coworkingId());

        Membership saved = membershipRepository.save(membership);
        return toDto(saved);
    }

    public List<MembershipDto> getAll() {
        return membershipRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public MembershipDto getById(Long id) {
        Membership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + id));
        return toDto(membership);
    }

    private void validateCreateRequest(CreateMembershipDto dto) {
        if (dto.coworkingId() == null) {
            throw new IllegalArgumentException("coworkingId must not be null");
        }
    }

    private MembershipDto toDto(Membership membership) {
        return new MembershipDto(
                membership.getId(),
                membership.getCoworkingId(),
                membership.getBalance(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }
}