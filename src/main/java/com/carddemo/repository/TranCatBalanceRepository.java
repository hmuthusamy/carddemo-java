package com.carddemo.repository;

import com.carddemo.model.TranCatBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for TranCatBalance entity (VSAM TCATBAL KSDS).
 * Mapped from COBOL copybook CVTRA01Y.cpy
 */
@Repository
public interface TranCatBalanceRepository extends JpaRepository<TranCatBalance, Long> {
}
