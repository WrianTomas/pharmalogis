package com.pharmalogis.system.modules.attendance.infrastructure;

import com.pharmalogis.system.modules.organization.infrastructure.EmployeeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "markations", uniqueConstraints = @UniqueConstraint(columnNames = { "employee_id", "occurred_at" }))
public class MarkationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public Long externalId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    public EmployeeEntity employee;
    @Column(name = "occurred_at", nullable = false)
    public LocalDateTime occurredAt;
}
