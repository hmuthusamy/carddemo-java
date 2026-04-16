package com.carddemo.repository;

import com.carddemo.model.TransactionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TransactionRepository – Spring Data JPA repository for TransactionData.
 *
 * Replaces CICS VSAM file operations against TRANSACT dataset:
 *   EXEC CICS WRITE   DATASET('TRANSACT') → save()
 *   EXEC CICS READ    DATASET('TRANSACT') → findById()
 *   EXEC CICS STARTBR / READPREV          → findTopByOrderByTranIdDesc()
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionData, String> {

    /**
     * Replicates the COBOL STARTBR(HIGH-VALUES) + READPREV pattern used to
     * obtain the most recent transaction ID before generating the next one.
     *
     * COBOL equivalent:
     *   MOVE HIGH-VALUES TO TRAN-ID
     *   EXEC CICS STARTBR DATASET('TRANSACT') RIDFLD(TRAN-ID) …
     *   EXEC CICS READPREV …
     *   ADD 1 TO WS-TRAN-ID-N
     */
    @Query("SELECT t FROM TransactionData t ORDER BY t.tranId DESC LIMIT 1")
    Optional<TransactionData> findTopByOrderByTranIdDesc();
}
