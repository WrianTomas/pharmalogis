package com.pharmalogis.system.modules.organization.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 8)
    public String document;
    @Column(nullable = false, length = 150)
    public String fullName;
    @Column(nullable = false, unique = true, length = 150)
    public String email;
    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    public DepartmentEntity department;
    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    public ShiftEntity shift;
    @Column(nullable = false)
    public boolean active;
}