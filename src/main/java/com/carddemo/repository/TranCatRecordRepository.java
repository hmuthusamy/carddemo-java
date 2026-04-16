package com.carddemo.repository;

import com.carddemo.model.TranCatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for TranCatRecord entity (VSAM TRANCATG KSDS).
 * Mapped from COBOL copybook CVTRA04Y.cpy
 */
@Repository
public interface TranCatRecordRepository extends JpaRepository<TranCatRecord, String> {
}
