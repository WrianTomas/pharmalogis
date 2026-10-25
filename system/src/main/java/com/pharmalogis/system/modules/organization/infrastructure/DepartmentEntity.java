package com.pharmalogis.system.modules.organization.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class DepartmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 100)
    public String name;
    @Column(nullable = false, length = 250)
    public String description;
}