package com.hse.userservice.internal.service;

import com.hse.userservice.feature.booking.service.BookingImpactService;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.internal.dto.deactivation.DayClosingCommitRequest;
import com.hse.userservice.internal.dto.deactivation.DayClosingPreviewRequest;
import com.hse.userservice.internal.dto.deactivation.OperationalImpactResponse;
import com.hse.userservice.internal.dto.deactivation.PlaceClosingCommitRequest;
import com.hse.userservice.internal.dto.deactivation.PlaceClosingPreviewRequest;
import com.hse.userservice.internal.dto.deactivation.PlaceDeactivationCommitRequest;
import com.hse.userservice.internal.dto.deactivation.PlaceDeactivationPreviewRequest;
import com.hse.userservice.internal.dto.deactivation.ScheduleReductionCommitRequest;
import com.hse.userservice.internal.dto.deactivation.ScheduleReductionPreviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeactivationInternalServiceUnitTest {
    @Mock BookingImpactService bookingImpactService;
    @Mock
    MembershipService membershipService;

    private UserDeactivationInternalService service;

    @BeforeEach
    void setUp() {
        service = new UserDeactivationInternalService(bookingImpactService, membershipService);
    }

    @Test
    void delegatesPlaceDeactivationPreviewAndCommit() {
        PlaceDeactivationPreviewRequest preview = new PlaceDeactivationPreviewRequest(10L);
        PlaceDeactivationCommitRequest commit = new PlaceDeactivationCommitRequest(10L, "commit-hash");
        OperationalImpactResponse previewResponse = response("preview");
        OperationalImpactResponse commitResponse = response("commit");
        when(bookingImpactService.previewPlaceDeactivation(9L, preview)).thenReturn(previewResponse);
        when(bookingImpactService.commitPlaceDeactivation(9L, commit)).thenReturn(commitResponse);

        assertThat(service.previewPlaceDeactivation(9L, preview)).isSameAs(previewResponse);
        assertThat(service.commitPlaceDeactivation(9L, commit)).isSameAs(commitResponse);

        verify(bookingImpactService).previewPlaceDeactivation(9L, preview);
        verify(bookingImpactService).commitPlaceDeactivation(9L, commit);
    }

    @Test
    void delegatesPlaceClosingDayClosingAndScheduleReduction() {
        PlaceClosingPreviewRequest placePreview = new PlaceClosingPreviewRequest(10L, LocalDate.of(2026, 6, 1));
        PlaceClosingCommitRequest placeCommit = new PlaceClosingCommitRequest(10L, LocalDate.of(2026, 6, 1), "p2");
        DayClosingPreviewRequest dayPreview = new DayClosingPreviewRequest(LocalDate.of(2026, 6, 2));
        DayClosingCommitRequest dayCommit = new DayClosingCommitRequest(LocalDate.of(2026, 6, 2), "d2");
        ScheduleReductionPreviewRequest schedulePreview = new ScheduleReductionPreviewRequest(
                List.of(LocalDate.of(2026, 6, 3))
        );
        ScheduleReductionCommitRequest scheduleCommit = new ScheduleReductionCommitRequest(
                List.of(LocalDate.of(2026, 6, 3)),
                "s2"
        );
        when(bookingImpactService.previewPlaceClosing(9L, placePreview)).thenReturn(response("place-preview"));
        when(bookingImpactService.commitPlaceClosing(9L, placeCommit)).thenReturn(response("place-commit"));
        when(bookingImpactService.previewDayClosing(9L, dayPreview)).thenReturn(response("day-preview"));
        when(bookingImpactService.commitDayClosing(9L, dayCommit)).thenReturn(response("day-commit"));
        when(bookingImpactService.previewScheduleReduction(9L, schedulePreview)).thenReturn(response("schedule-preview"));
        when(bookingImpactService.commitScheduleReduction(9L, scheduleCommit)).thenReturn(response("schedule-commit"));

        assertThat(service.previewPlaceClosing(9L, placePreview).impactHash()).isEqualTo("place-preview");
        assertThat(service.commitPlaceClosing(9L, placeCommit).impactHash()).isEqualTo("place-commit");
        assertThat(service.previewDayClosing(9L, dayPreview).impactHash()).isEqualTo("day-preview");
        assertThat(service.commitDayClosing(9L, dayCommit).impactHash()).isEqualTo("day-commit");
        assertThat(service.previewScheduleReduction(9L, schedulePreview).impactHash()).isEqualTo("schedule-preview");
        assertThat(service.commitScheduleReduction(9L, scheduleCommit).impactHash()).isEqualTo("schedule-commit");
    }


    @Test
    void previewMembershipBlockValidatesMembershipBoundaryThenDelegatesToBookingImpactService() {
        OperationalImpactResponse response = response("block-preview");
        when(bookingImpactService.previewMembershipBlock(9L, 77L)).thenReturn(response);

        assertThat(service.previewMembershipBlock(9L, 77L)).isSameAs(response);

        verify(membershipService).requireMembershipInCoworking(9L, 77L);
        verify(bookingImpactService).previewMembershipBlock(9L, 77L);
    }

    @Test
    void commitMembershipBlockLocksMembershipCommitsImpactAndBlocksMembership() {
        OperationalImpactResponse response = response("block-commit");
        when(bookingImpactService.commitMembershipBlock(9L, 77L, "impact-hash")).thenReturn(response);

        assertThat(service.commitMembershipBlock(9L, 77L, "impact-hash")).isSameAs(response);

        verify(membershipService).requireMembershipInCoworkingForUpdate(9L, 77L);
        verify(bookingImpactService).commitMembershipBlock(9L, 77L, "impact-hash");
        verify(membershipService).blockByAdmin(9L, 77L);
    }

    private static OperationalImpactResponse response(String hash) {
        return new OperationalImpactResponse(0, List.of(), List.of(), 0L, hash);
    }
}
