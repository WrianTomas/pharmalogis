package com.pharmalogis.system.modules.integration.application;

import com.pharmalogis.system.modules.integration.infrastructure.*;
import com.pharmalogis.system.modules.attendance.infrastructure.*;
import com.pharmalogis.system.modules.organization.infrastructure.EmployeeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ImportMarkationsHandler {
    public record Command(LocalDate date) {
    }

    public record Result(int imported, int duplicates, int unknownEmployees) {
    }

    private final ExternalMarkationJpaRepository external;
    private final MarkationJpaRepository internal;
    private final EmployeeJpaRepository employees;

    @Transactional
    public Result handle(Command command) {
        int imported = 0, duplicates = 0, unknown = 0;
        var allEmployees = employees.findAll();
        for (var mark : external.findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderById(
                command.date().atStartOfDay(), command.date().plusDays(1).atStartOfDay())) {
            var employee = allEmployees.stream().filter(e -> e.document.equals(mark.employeeDocument)).findFirst();
            if (employee.isEmpty()) {
                unknown++;
                continue;
            }
            if (internal.existsByExternalId(mark.id)
                    || internal.existsByEmployeeIdAndOccurredAt(employee.get().id, mark.occurredAt)) {
                duplicates++;
                continue;
            }
            var e = new MarkationEntity();
            e.externalId = mark.id;
            e.employee = employee.get();
            e.occurredAt = mark.occurredAt;
            internal.saveAndFlush(e);
            imported++;
        }
        return new Result(imported, duplicates, unknown);
    }
}
