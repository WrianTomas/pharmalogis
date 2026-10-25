package com.pharmalogis.system.modules.integration.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "markations_from_external_provider")
public class ExternalMarkationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 8)
    public String employeeDocument;
    @Column(nullable = false)
    public LocalDateTime occurredAt;
}
