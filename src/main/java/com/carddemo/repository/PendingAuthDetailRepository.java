package com.carddemo.repository;

import com.carddemo.model.PendingAuthDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

/**
 * JPA Repository for PendingAuthDetail entity (IMS PADET segment).
 * Mapped from COBOL copybook CIPAUDTY.cpy
 */
@Repository
public interface PendingAuthDetailRepository extends JpaRepository<PendingAuthDetail, BigDecimal> {
}
