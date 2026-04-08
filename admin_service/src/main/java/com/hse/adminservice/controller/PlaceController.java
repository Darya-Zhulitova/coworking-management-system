package com.hse.adminservice.controller;

import com.hse.adminservice.dto.PlaceCreateRequest;
import com.hse.adminservice.dto.PlaceDeactivationPreviewResponse;
import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.dto.PlaceUpdateRequest;
import com.hse.adminservice.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(@PathVariable Long coworkingId, @Valid @RequestBody PlaceCreateRequest request) {
        return placeService.create(coworkingId, request);
    }

    @GetMapping
    public List<PlaceResponse> getAll(@PathVariable Long coworkingId, @RequestParam(required = false) Long placeTypeId) {
        return placeService.getAll(coworkingId, placeTypeId);
    }

    @GetMapping("/{placeId}")
    public PlaceResponse getById(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.getById(coworkingId, placeId);
    }

    @PutMapping("/{placeId}")
    public PlaceResponse update(@PathVariable Long coworkingId, @PathVariable Long placeId, @Valid @RequestBody PlaceUpdateRequest request) {
        return placeService.update(coworkingId, placeId, request);
    }

    @PostMapping("/{placeId}/deactivate")
    public PlaceDeactivationPreviewResponse deactivate(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.deactivate(coworkingId, placeId);
    }

    @PostMapping("/{placeId}/activate")
    public PlaceResponse activate(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.activate(coworkingId, placeId);
    }

    @DeleteMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        placeService.archive(coworkingId, placeId);
    }
}
