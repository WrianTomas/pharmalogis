package com.pharmalogis.system.modules.organization.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "shifts")
public class ShiftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 100)
    public String name;
    @Column(nullable = false)
    public java.time.LocalTime startTime;
    @Column(nullable = false)
    public java.time.LocalTime endTime;
    @Column(nullable = false)
    public int toleranceMinutes;
}