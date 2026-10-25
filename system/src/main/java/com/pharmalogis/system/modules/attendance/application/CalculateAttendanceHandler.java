package com.pharmalogis.system.modules.attendance.application;

import com.pharmalogis.system.modules.attendance.domain.AttendancePolicy;
import com.pharmalogis.system.modules.attendance.infrastructure.*;
import com.pharmalogis.system.modules.organization.infrastructure.EmployeeJpaRepository;
import com.pharmalogis.system.modules.organization.application.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
@RequiredArgsConstructor
public class CalculateAttendanceHandler {
    public record Command(LocalDate date) {
    }

    private final EmployeeJpaRepository employees;
    private final MarkationJpaRepository marks;
    private final AttendanceJpaRepository attendances;

    @Transactional
    public int handle(Command command) {
        LocalDate date = command.date();
        if (date.isAfter(LocalDate.now(ZoneId.of("America/Lima"))))
            throw new BusinessException("No se puede calcular una fecha futura.");
        int count = 0;
        for (var employee : employees.findAll()) {
            if (!employee.active)
                continue;
            var shift = employee.shift;
            var timestamps = marks
                    .findByEmployeeIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAt(employee.id,
                            date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                    .stream().map(m -> m.occurredAt).toList();
            var result = new AttendancePolicy().calculate(date, shift.startTime, shift.endTime, shift.toleranceMinutes,
                    timestamps);
            var row = attendances.findByEmployeeIdAndWorkDate(employee.id, date).orElseGet(AttendanceEntity::new);
            row.employee = employee;
            row.workDate = date;
            row.firstIn = result.firstIn();
            row.lastOut = result.lastOut();
            row.status = result.status();
            row.lateMinutes = result.lateMinutes();
            row.workedMinutes = result.workedMinutes();
            row.incidents = result.incidents();
            attendances.saveAndFlush(row);
            count++;
        }
        return count;
    }
}
