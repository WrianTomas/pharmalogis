package com.pharmalogis.system.modules.organization.domain;

import java.time.LocalTime;

public record Shift(Long id, String name, LocalTime startTime, LocalTime endTime, int toleranceMinutes) {
}
