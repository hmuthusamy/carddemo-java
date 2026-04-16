package com.carddemo.repository;

import com.carddemo.model.TranTypeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for TranTypeRecord entity (VSAM TRANTYPE KSDS).
 * Mapped from COBOL copybook CVTRA03Y.cpy
 */
@Repository
public interface TranTypeRecordRepository extends JpaRepository<TranTypeRecord, String> {
}
