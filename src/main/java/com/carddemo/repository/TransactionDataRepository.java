package com.carddemo.repository;

import com.carddemo.model.TransactionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link TransactionData}.
 * Used by the Spring Batch reader in cbtrn02cJob.
 */
@Repository
public interface TransactionDataRepository extends JpaRepository<TransactionData, String> {

    /**
     * Find all transactions with a given status, ordered for deterministic processing.
     * Spring Batch JpaPagingItemReader uses JPQL query against this status filter.
     */
    List<TransactionData> findByTranStatusOrderByTranOrigTsAsc(String tranStatus);
}
