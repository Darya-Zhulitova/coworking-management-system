package com.hse.adminservice.controller;

import com.hse.adminservice.dto.CoworkingCreateRequest;
import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.dto.CoworkingUpdateRequest;
import com.hse.adminservice.service.CoworkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public List<CoworkingResponse> getAll() {
        return coworkingService.getAll();
    }

    @GetMapping("/{id}")
    public CoworkingResponse getById(@PathVariable Long id) {
        return coworkingService.getById(id);
    }

    @PutMapping("/{id}")
    public CoworkingResponse update(@PathVariable Long id, @Valid @RequestBody CoworkingUpdateRequest request) {
        return coworkingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        coworkingService.archive(id);
    }
}