package com.carddemo.repository;

import com.carddemo.model.TransactionCategoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TransactionCategoryBalance — mirrors TCATBAL-FILE (CVTRA01Y).
 * Spring Batch will use a JpaCursorItemReader to read records sequentially,
 * preserving the COBOL sequential-read behaviour of CBACT04C.
 */
@Repository
public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalance.TransactionCategoryKey> {
}
