package com.carddemo.repository;

import com.carddemo.model.TransactionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for TransactionData entity (VSAM TRAN KSDS).
 * Mapped from COBOL copybook CVTRA05Y.cpy
 */
@Repository
public interface TransactionDataRepository extends JpaRepository<TransactionData, String> {
}
