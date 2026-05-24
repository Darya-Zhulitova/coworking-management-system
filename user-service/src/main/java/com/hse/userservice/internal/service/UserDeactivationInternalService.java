package com.hse.userservice.internal.service;

import com.hse.userservice.feature.booking.service.BookingImpactService;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.internal.dto.deactivation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeactivationInternalService {
    private final BookingImpactService bookingImpactService;
    private final MembershipService membershipService;

    public OperationalImpactResponse previewPlaceDeactivation(
            Long coworkingId,
            PlaceDeactivationPreviewRequest request
    ) {
        return bookingImpactService.previewPlaceDeactivation(coworkingId, request);
    }

    @Transactional
    public OperationalImpactResponse commitPlaceDeactivation(Long coworkingId, PlaceDeactivationCommitRequest request) {
        return bookingImpactService.commitPlaceDeactivation(coworkingId, request);
    }

    public OperationalImpactResponse previewPlaceClosing(Long coworkingId, PlaceClosingPreviewRequest request) {
        return bookingImpactService.previewPlaceClosing(coworkingId, request);
    }

    @Transactional
    public OperationalImpactResponse commitPlaceClosing(Long coworkingId, PlaceClosingCommitRequest request) {
        return bookingImpactService.commitPlaceClosing(coworkingId, request);
    }

    public OperationalImpactResponse previewDayClosing(Long coworkingId, DayClosingPreviewRequest request) {
        return bookingImpactService.previewDayClosing(coworkingId, request);
    }

    @Transactional
    public OperationalImpactResponse commitDayClosing(Long coworkingId, DayClosingCommitRequest request) {
        return bookingImpactService.commitDayClosing(coworkingId, request);
    }

    public OperationalImpactResponse previewScheduleReduction(
            Long coworkingId,
            ScheduleReductionPreviewRequest request
    ) {
        return bookingImpactService.previewScheduleReduction(coworkingId, request);
    }

    @Transactional
    public OperationalImpactResponse commitScheduleReduction(Long coworkingId, ScheduleReductionCommitRequest request) {
        return bookingImpactService.commitScheduleReduction(coworkingId, request);
    }

    public OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId) {
        membershipService.requireMembershipInCoworking(coworkingId, membershipId);
        return bookingImpactService.previewMembershipBlock(coworkingId, membershipId);
    }

    @Transactional
    public OperationalImpactResponse commitMembershipBlock(Long coworkingId, Long membershipId, String impactHash) {
        membershipService.requireMembershipInCoworkingForUpdate(coworkingId, membershipId);
        OperationalImpactResponse response = bookingImpactService.commitMembershipBlock(
                coworkingId,
                membershipId,
                impactHash
        );
        membershipService.blockByAdmin(coworkingId, membershipId);
        return response;
    }

}
