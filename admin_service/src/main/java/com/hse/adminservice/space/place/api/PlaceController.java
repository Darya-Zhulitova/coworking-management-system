package com.hse.adminservice.space.place.api;

import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.application.PlaceService;
import com.hse.adminservice.space.place.dto.PlaceCreateRequest;
import com.hse.adminservice.space.place.dto.PlaceResponse;
import com.hse.adminservice.space.place.dto.PlaceUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public List<PlaceResponse> getAll(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) Long placeTypeId
    ) {
        return placeService.getAll(coworkingId, placeTypeId);
    }

    @GetMapping("/{placeId}/bookings")
    public PlaceBookingListResponse getBookings(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.getBookings(coworkingId, placeId);
    }

    @GetMapping("/{placeId}")
    public PlaceResponse getById(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.getById(coworkingId, placeId);
    }

    @PostMapping("/{placeId}/photo")
    public PlaceResponse uploadPhoto(
            @PathVariable Long coworkingId,
            @PathVariable Long placeId,
            @RequestParam("file") MultipartFile file
    ) {
        return placeService.uploadPhoto(coworkingId, placeId, file);
    }

    @PutMapping("/{placeId}")
    public PlaceResponse update(
            @PathVariable Long coworkingId,
            @PathVariable Long placeId,
            @Valid @RequestBody PlaceUpdateRequest request
    ) {
        return placeService.update(coworkingId, placeId, request);
    }

    @PostMapping("/{placeId}/deactivation/preview")
    public OperationalImpactResponse previewDeactivate(@PathVariable Long coworkingId, @PathVariable Long placeId) {
        return placeService.previewDeactivate(coworkingId, placeId);
    }

    @PostMapping("/{placeId}/deactivation/commit")
    public OperationalImpactResponse commitDeactivate(
            @PathVariable Long coworkingId,
            @PathVariable Long placeId,
            @Valid @RequestBody ImpactCommitRequest request
    ) {
        return placeService.commitDeactivate(coworkingId, placeId, request.getImpactHash());
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
