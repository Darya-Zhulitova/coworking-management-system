package com.hse.adminservice.configlookup.api;

import com.hse.adminservice.configlookup.application.CoworkingConfigurationLookupService;
import com.hse.adminservice.configlookup.dto.BookingContextResponse;
import com.hse.adminservice.configlookup.dto.PlaceSummaryResponse;
import com.hse.adminservice.configlookup.dto.ServiceRequestTypeLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/internal/coworkings")
@RequiredArgsConstructor
public class AdminConfigurationLookupController {
    private final CoworkingConfigurationLookupService configurationLookupService;

    @GetMapping("/{coworkingId}/booking-context")
    public BookingContextResponse getBookingContext(@PathVariable Long coworkingId) {
        return configurationLookupService.getBookingContext(coworkingId);
    }

    @GetMapping("/{coworkingId}/service-request-types")
    public List<ServiceRequestTypeLookupResponse> getServiceRequestTypes(@PathVariable Long coworkingId) {
        return configurationLookupService.getServiceRequestTypes(coworkingId);
    }

    @GetMapping("/{coworkingId}/service-request-types/{typeId}")
    public ServiceRequestTypeLookupResponse getServiceRequestType(
            @PathVariable Long coworkingId,
            @PathVariable Long typeId
    ) {
        return configurationLookupService.getServiceRequestType(coworkingId, typeId);
    }

    @GetMapping("/{coworkingId}/places/summary")
    public List<PlaceSummaryResponse> getPlaceSummaries(
            @PathVariable Long coworkingId,
            @RequestParam(name = "ids", required = false, defaultValue = "") String ids
    ) {
        List<Long> placeIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Long::valueOf)
                .toList();
        return configurationLookupService.getPlaceSummaries(coworkingId, placeIds);
    }
}
