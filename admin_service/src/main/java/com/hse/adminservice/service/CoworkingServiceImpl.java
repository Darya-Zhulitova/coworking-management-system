package com.hse.adminservice.service;

import com.hse.adminservice.dto.CoworkingCreateRequest;
import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.dto.CoworkingUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoworkingServiceImpl implements CoworkingService {

    private final CoworkingRepository coworkingRepository;

    @Override
    public CoworkingResponse create(CoworkingCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Coworking coworking = Coworking.builder().name(request.getName()).active(true).archived(false).archivedAt(null).createdAt(now).updatedAt(now).build();

        return mapToResponse(coworkingRepository.save(coworking));
    }

    @Override
    public List<CoworkingResponse> getAll() {
        return coworkingRepository.findAllByArchivedFalse().stream().map(this::mapToResponse).toList();
    }

    @Override
    public CoworkingResponse getById(Long id) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id).orElseThrow(() -> new RuntimeException("Coworking not found"));

        return mapToResponse(coworking);
    }

    @Override
    public CoworkingResponse update(Long id, CoworkingUpdateRequest request) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id).orElseThrow(() -> new RuntimeException("Coworking not found"));

        coworking.setName(request.getName());
        if (request.getActive() != null) {
            coworking.setActive(request.getActive());
        }
        coworking.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(coworkingRepository.save(coworking));
    }

    @Override
    public void archive(Long id) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id).orElseThrow(() -> new RuntimeException("Coworking not found"));

        coworking.setArchived(true);
        coworking.setArchivedAt(LocalDateTime.now());
        coworking.setActive(false);
        coworking.setUpdatedAt(LocalDateTime.now());

        coworkingRepository.save(coworking);
    }

    private CoworkingResponse mapToResponse(Coworking coworking) {
        return CoworkingResponse.builder().id(coworking.getId()).name(coworking.getName()).active(coworking.getActive()).archived(coworking.getArchived()).archivedAt(coworking.getArchivedAt()).createdAt(coworking.getCreatedAt()).updatedAt(coworking.getUpdatedAt()).build();
    }
}