package com.pharmalogis.system.modules.attendance.infrastructure;

import com.pharmalogis.system.modules.organization.infrastructure.EmployeeEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(columnNames = { "employee_id", "work_date" }))
public class AttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    public EmployeeEntity employee;
    @Column(name = "work_date", nullable = false)
    public LocalDate workDate;
    public LocalDateTime firstIn;
    public LocalDateTime lastOut;
    @Column(nullable = false, length = 20)
    public String status;
    @Column(nullable = false)
    public int lateMinutes;
    @Column(nullable = false)
    public int workedMinutes;
    @Column(nullable = false, length = 250)
    public String incidents;
}
