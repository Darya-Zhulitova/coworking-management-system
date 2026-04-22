package com.hse.adminservice.schedule.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class PlaceClosingResponse {
    Long id;
    Long placeId;
    String placeName;
    Long floorId;
    LocalDate date;
    String name;
    Boolean active;
}
