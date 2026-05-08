package com.hse.adminservice.space.floor.api;

import com.hse.adminservice.space.floor.application.FloorService;
import com.hse.adminservice.space.floor.dto.FloorCreateRequest;
import com.hse.adminservice.space.floor.dto.FloorResponse;
import com.hse.adminservice.space.floor.dto.FloorUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/floors")
@RequiredArgsConstructor
public class FloorController {
    private final FloorService floorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse create(@PathVariable Long coworkingId, @Valid @RequestBody FloorCreateRequest request) {
        return floorService.create(coworkingId, request);
    }

    @GetMapping
    public List<FloorResponse> getAll(@PathVariable Long coworkingId) {
        return floorService.getAll(coworkingId);
    }

    @GetMapping("/{floorId}")
    public FloorResponse getById(@PathVariable Long coworkingId, @PathVariable Long floorId) {
        return floorService.getById(coworkingId, floorId);
    }

    @PutMapping("/{floorId}")
    public FloorResponse update(
            @PathVariable Long coworkingId,
            @PathVariable Long floorId,
            @Valid @RequestBody FloorUpdateRequest request
    ) {
        return floorService.update(coworkingId, floorId, request);
    }

    @DeleteMapping("/{floorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long coworkingId, @PathVariable Long floorId) {
        floorService.archive(coworkingId, floorId);
    }
}
