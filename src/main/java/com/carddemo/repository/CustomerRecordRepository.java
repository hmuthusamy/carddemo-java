package com.carddemo.repository;

import com.carddemo.model.CustomerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for CustomerRecord entity (VSAM CUSTDAT KSDS).
 * Mapped from COBOL copybook CUSTREC.cpy
 */
@Repository
public interface CustomerRecordRepository extends JpaRepository<CustomerRecord, Long> {
}
