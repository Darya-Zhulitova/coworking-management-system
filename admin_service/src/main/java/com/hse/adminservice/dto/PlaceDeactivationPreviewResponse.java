package com.hse.adminservice.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PlaceDeactivationPreviewResponse {
    Long placeId;
    String placeName;
    boolean activeBeforeChange;
    int simulatedAffectedFutureBookings;
    List<String> plannedUserDomainCommands;
    String mode;
}
