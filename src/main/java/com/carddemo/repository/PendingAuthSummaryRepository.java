package com.carddemo.repository;

import com.carddemo.model.PendingAuthSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

/**
 * JPA Repository for PendingAuthSummary entity (IMS PASUM segment).
 * Mapped from COBOL copybook CIPAUSMY.cpy
 */
@Repository
public interface PendingAuthSummaryRepository extends JpaRepository<PendingAuthSummary, BigDecimal> {
}
