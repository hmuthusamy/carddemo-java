package com.carddemo.repository;

import com.carddemo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Transaction — mirrors TRANSACT-FILE (CVTRA05Y).
 * Used by the Spring Batch ItemWriter in CBACT04C to persist generated
 * interest transactions (paragraph 1300-B-WRITE-TX).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
