package com.pharmalogis.system.modules.attendance.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MarkationJpaRepository extends JpaRepository<MarkationEntity, Long> {
    boolean existsByExternalId(Long id);

    boolean existsByEmployeeIdAndOccurredAt(Long employeeId, LocalDateTime occurredAt);

    List<MarkationEntity> findByEmployeeIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAt(
            Long employeeId, LocalDateTime from, LocalDateTime to);
}
