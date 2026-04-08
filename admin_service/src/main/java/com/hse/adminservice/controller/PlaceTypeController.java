package com.hse.adminservice.controller;

import com.hse.adminservice.dto.PlaceTypeCreateRequest;
import com.hse.adminservice.dto.PlaceTypeResponse;
import com.hse.adminservice.dto.PlaceTypeUpdateRequest;
import com.hse.adminservice.service.PlaceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/place-types")
@RequiredArgsConstructor
public class PlaceTypeController {

    private final PlaceTypeService placeTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceTypeResponse create(@PathVariable Long coworkingId, @Valid @RequestBody PlaceTypeCreateRequest request) {
        return placeTypeService.create(coworkingId, request);
    }

    @GetMapping
    public List<PlaceTypeResponse> getAll(@PathVariable Long coworkingId) {
        return placeTypeService.getAll(coworkingId);
    }

    @GetMapping("/{placeTypeId}")
    public PlaceTypeResponse getById(@PathVariable Long coworkingId, @PathVariable Long placeTypeId) {
        return placeTypeService.getById(coworkingId, placeTypeId);
    }

    @PutMapping("/{placeTypeId}")
    public PlaceTypeResponse update(@PathVariable Long coworkingId, @PathVariable Long placeTypeId, @Valid @RequestBody PlaceTypeUpdateRequest request) {
        return placeTypeService.update(coworkingId, placeTypeId, request);
    }

    @DeleteMapping("/{placeTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long coworkingId, @PathVariable Long placeTypeId) {
        placeTypeService.archive(coworkingId, placeTypeId);
    }
}
