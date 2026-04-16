package com.carddemo.repository;

import com.carddemo.model.DailyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for DailyTransaction entity (VSAM DALYTRAN KSDS).
 * Mapped from COBOL copybook CVTRA06Y.cpy
 */
@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, String> {
}
