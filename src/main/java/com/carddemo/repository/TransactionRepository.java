package com.carddemo.repository;

import com.carddemo.model.TransactionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TransactionData}.
 *
 * <p>Replaces the direct VSAM/KSDS file I/O performed by CBTRN01C against
 * TRANFILE (FD-TRANS-ID keyed sequential file). Spring Batch's
 * {@code JpaItemWriter} uses {@code save()} / {@code saveAll()} from this
 * repository to persist processed transaction records.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionData, String> {
}
