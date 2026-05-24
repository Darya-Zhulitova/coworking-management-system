package com.hse.adminservice.coworking.api;

import com.hse.adminservice.coworking.application.CoworkingService;
import com.hse.adminservice.coworking.dto.CoworkingCreateRequest;
import com.hse.adminservice.coworking.dto.CoworkingDashboardResponse;
import com.hse.adminservice.coworking.dto.CoworkingResponse;
import com.hse.adminservice.coworking.dto.CoworkingUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings")
@RequiredArgsConstructor
public class CoworkingController {
    private final CoworkingService coworkingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkingResponse create(@Valid @RequestBody CoworkingCreateRequest request) {
        return coworkingService.create(request);
    }

    @GetMapping
    public List<CoworkingResponse> getAll(@RequestParam(name = "archived", defaultValue = "false") boolean archived) {
        return archived ? coworkingService.getArchived() : coworkingService.getAll();
    }

    @GetMapping("/{id}")
    public CoworkingResponse getById(@PathVariable Long id) {
        return coworkingService.getById(id);
    }

    @GetMapping("/{id}/dashboard")
    public CoworkingDashboardResponse getDashboard(@PathVariable Long id) {
        return coworkingService.getDashboard(id);
    }

    @PutMapping("/{id}")
    public CoworkingResponse update(@PathVariable Long id, @Valid @RequestBody CoworkingUpdateRequest request) {
        return coworkingService.update(id, request);
    }

    @PostMapping("/{id}/photos")
    public CoworkingResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return coworkingService.uploadPhoto(id, file);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        coworkingService.archive(id);
    }
}