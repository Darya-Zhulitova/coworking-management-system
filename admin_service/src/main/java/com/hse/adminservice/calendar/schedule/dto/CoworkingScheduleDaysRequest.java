package com.hse.adminservice.calendar.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoworkingScheduleDaysRequest {
    @NotNull
    private Boolean monday;
    @NotNull
    private Boolean tuesday;
    @NotNull
    private Boolean wednesday;
    @NotNull
    private Boolean thursday;
    @NotNull
    private Boolean friday;
    @NotNull
    private Boolean saturday;
    @NotNull
    private Boolean sunday;
    private String impactHash;
}
