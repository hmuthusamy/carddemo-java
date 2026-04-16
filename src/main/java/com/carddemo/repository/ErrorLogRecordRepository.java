package com.carddemo.repository;

import com.carddemo.model.ErrorLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for ErrorLogRecord entity (VSAM ERRLOG KSDS).
 * Mapped from COBOL copybook CCPAUERY.cpy
 */
@Repository
public interface ErrorLogRecordRepository extends JpaRepository<ErrorLogRecord, String> {
}
