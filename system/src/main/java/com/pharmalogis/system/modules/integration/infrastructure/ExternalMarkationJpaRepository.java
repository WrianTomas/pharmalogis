package com.pharmalogis.system.modules.integration.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ExternalMarkationJpaRepository extends JpaRepository<ExternalMarkationEntity, Long> {
    List<ExternalMarkationEntity> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderById(LocalDateTime from,
            LocalDateTime to);
}
