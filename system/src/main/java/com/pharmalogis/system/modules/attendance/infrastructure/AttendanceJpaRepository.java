package com.pharmalogis.system.modules.attendance.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.time.LocalDate;
import java.util.*;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, Long> {
    Optional<AttendanceEntity> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate date);

    @EntityGraph(attributePaths = { "employee", "employee.department", "employee.shift" })
    List<AttendanceEntity> findByWorkDateOrderByEmployeeFullName(LocalDate date);
}
