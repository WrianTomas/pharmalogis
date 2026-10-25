package com.pharmalogis.system.modules.attendance.application;

import com.pharmalogis.system.modules.attendance.infrastructure.AttendanceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAttendanceHandler {
    public record Query(LocalDate date, Long employeeId, String status) {
    }

    public record Row(String employee, String document, String department, LocalDateTime firstIn, LocalDateTime lastOut,
            String status, int lateMinutes, int workedMinutes, String incidents) {
    }

    private final AttendanceJpaRepository repository;

    @Transactional(readOnly = true)
    public List<Row> handle(Query query) {
        return repository.findByWorkDateOrderByEmployeeFullName(query.date()).stream()
                .filter(r -> query.employeeId() == null || r.employee.id.equals(query.employeeId()))
                .filter(r -> query.status() == null || query.status().isBlank() || r.status.equals(query.status()))
                .map(r -> new Row(r.employee.fullName, r.employee.document, r.employee.department.name, r.firstIn,
                        r.lastOut, r.status, r.lateMinutes, r.workedMinutes, r.incidents))
                .toList();
    }
}
