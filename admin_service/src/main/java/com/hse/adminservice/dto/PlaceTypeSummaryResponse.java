package com.hse.adminservice.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlaceTypeSummaryResponse {
    Long id;
    String code;
    String name;
    Boolean active;
}
