package com.hse.adminservice.service;

import com.hse.adminservice.dto.PlaceCreateRequest;
import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.dto.PlaceUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.entity.Place;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final CoworkingRepository coworkingRepository;

    @Override
    public PlaceResponse create(Long coworkingId, PlaceCreateRequest request) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId).orElseThrow(() -> new RuntimeException("Coworking not found"));

        if (placeRepository.existsByCoworkingIdAndNameAndArchivedFalse(coworkingId, request.getName())) {
            throw new RuntimeException("Place name must be unique within coworking");
        }

        LocalDateTime now = LocalDateTime.now();

        Place place = Place.builder().name(request.getName()).type(request.getType()).coworking(coworking).active(true).archived(false).archivedAt(null).createdAt(now).updatedAt(now).build();

        return map(placeRepository.save(place));
    }

    @Override
    public List<PlaceResponse> getAll(Long coworkingId) {
        return placeRepository.findAllByCoworkingIdAndArchivedFalse(coworkingId).stream().map(this::map).toList();
    }

    @Override
    public PlaceResponse getById(Long coworkingId, Long placeId) {
        Place place = placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId).orElseThrow(() -> new RuntimeException("Place not found"));

        return map(place);
    }

    @Override
    public PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request) {
        Place place = placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId).orElseThrow(() -> new RuntimeException("Place not found"));

        if (!place.getName().equals(request.getName()) && placeRepository.existsByCoworkingIdAndNameAndArchivedFalse(coworkingId, request.getName())) {
            throw new RuntimeException("Place name must be unique within coworking");
        }

        place.setName(request.getName());

        if (request.getType() != null) {
            place.setType(request.getType());
        }

        if (request.getActive() != null) {
            place.setActive(request.getActive());
        }

        place.setUpdatedAt(LocalDateTime.now());

        return map(placeRepository.save(place));
    }

    @Override
    public void archive(Long coworkingId, Long placeId) {
        Place place = placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId).orElseThrow(() -> new RuntimeException("Place not found"));

        place.setArchived(true);
        place.setArchivedAt(LocalDateTime.now());
        place.setActive(false);
        place.setUpdatedAt(LocalDateTime.now());

        placeRepository.save(place);
    }

    private PlaceResponse map(Place place) {
        return PlaceResponse.builder().id(place.getId()).name(place.getName()).type(place.getType()).active(place.getActive()).archived(place.getArchived()).archivedAt(place.getArchivedAt()).createdAt(place.getCreatedAt()).updatedAt(place.getUpdatedAt()).coworkingId(place.getCoworking().getId()).build();
    }
}
