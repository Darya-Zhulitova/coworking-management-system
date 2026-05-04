package com.hse.adminservice.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();

    LocalDate today();
}
