package com.pharmalogis.system.modules.organization.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Long> {
}