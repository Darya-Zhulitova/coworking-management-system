package com.hse.adminservice.tariff.controller;

import com.hse.adminservice.tariff.dto.TariffCreateRequest;
import com.hse.adminservice.tariff.dto.TariffResponse;
import com.hse.adminservice.tariff.dto.TariffUpdateRequest;
import com.hse.adminservice.tariff.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/tariffs")
@RequiredArgsConstructor
public class TariffController {
    private final TariffService tariffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse create(@PathVariable Long coworkingId, @Valid @RequestBody TariffCreateRequest request) {
        return tariffService.create(coworkingId, request);
    }

    @GetMapping
    public List<TariffResponse> getAll(@PathVariable Long coworkingId) {
        return tariffService.getAll(coworkingId);
    }

    @GetMapping("/{tariffId}")
    public TariffResponse getById(@PathVariable Long coworkingId, @PathVariable Long tariffId) {
        return tariffService.getById(coworkingId, tariffId);
    }

    @PutMapping("/{tariffId}")
    public TariffResponse update(
            @PathVariable Long coworkingId,
            @PathVariable Long tariffId,
            @Valid @RequestBody TariffUpdateRequest request
    ) {
        return tariffService.update(coworkingId, tariffId, request);
    }

    @DeleteMapping("/{tariffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long coworkingId, @PathVariable Long tariffId) {
        tariffService.archive(coworkingId, tariffId);
    }
}
