package com.pharmalogis.system.modules.organization.domain;

public record Employee(Long id, String document, String fullName, String email, Long departmentId, Long shiftId,
        boolean active) {
}
