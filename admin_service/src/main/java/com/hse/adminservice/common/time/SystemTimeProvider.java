package com.hse.adminservice.common.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SystemTimeProvider implements TimeProvider {
    private final Clock clock;

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
