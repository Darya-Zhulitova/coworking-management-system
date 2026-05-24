package com.hse.userservice.internal.controller;

import com.hse.userservice.internal.dto.deactivation.*;
import com.hse.userservice.internal.dto.membershipblock.MembershipBlockCommitRequest;
import com.hse.userservice.internal.service.UserDeactivationInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/coworkings/{coworkingId}/booking-impact")
@RequiredArgsConstructor
public class UserDeactivationInternalController {
    private final UserDeactivationInternalService service;

    @PostMapping("/place-deactivation/preview")
    public OperationalImpactResponse previewPlaceDeactivation(
            @PathVariable Long coworkingId,
            @Valid @RequestBody
            PlaceDeactivationPreviewRequest request
    ) {
        return service.previewPlaceDeactivation(coworkingId, request);
    }

    @PostMapping("/place-deactivation/commit")
    public OperationalImpactResponse commitPlaceDeactivation(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceDeactivationCommitRequest request
    ) {
        return service.commitPlaceDeactivation(coworkingId, request);
    }

    @PostMapping("/place-closing/preview")
    public OperationalImpactResponse previewPlaceClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceClosingPreviewRequest request
    ) {
        return service.previewPlaceClosing(coworkingId, request);
    }

    @PostMapping("/place-closing/commit")
    public OperationalImpactResponse commitPlaceClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceClosingCommitRequest request
    ) {
        return service.commitPlaceClosing(coworkingId, request);
    }

    @PostMapping("/day-closing/preview")
    public OperationalImpactResponse previewDayClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody DayClosingPreviewRequest request
    ) {
        return service.previewDayClosing(coworkingId, request);
    }

    @PostMapping("/day-closing/commit")
    public OperationalImpactResponse commitDayClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody DayClosingCommitRequest request
    ) {
        return service.commitDayClosing(coworkingId, request);
    }

    @PostMapping("/schedule-reduction/preview")
    public OperationalImpactResponse previewScheduleReduction(
            @PathVariable Long coworkingId,
            @Valid @RequestBody
            ScheduleReductionPreviewRequest request
    ) {
        return service.previewScheduleReduction(coworkingId, request);
    }

    @PostMapping("/schedule-reduction/commit")
    public OperationalImpactResponse commitScheduleReduction(
            @PathVariable Long coworkingId,
            @Valid @RequestBody ScheduleReductionCommitRequest request
    ) {
        return service.commitScheduleReduction(coworkingId, request);
    }

    @PostMapping("/membership-block/{membershipId}/preview")
    public OperationalImpactResponse previewMembershipBlock(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId
    ) {
        return service.previewMembershipBlock(coworkingId, membershipId);
    }

    @PostMapping("/membership-block/{membershipId}/commit")
    public OperationalImpactResponse commitMembershipBlock(
            @PathVariable Long coworkingId,
            @PathVariable Long membershipId,
            @Valid @RequestBody MembershipBlockCommitRequest request
    ) {
        return service.commitMembershipBlock(coworkingId, membershipId, request.impactHash());
    }

}
