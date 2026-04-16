package com.carddemo.repository;

import com.carddemo.model.TransactionData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Spring Data JPA repository for {@link TransactionData}.
 *
 * <p>Replaces the CICS VSAM browse cycle used in COTRN00C (COTRNLIC):
 * <ul>
 *   <li>STARTBR  → open a keyed cursor / begin a query</li>
 *   <li>READNEXT → iterate forwards  (page forward)</li>
 *   <li>READPREV → iterate backwards (page backward)</li>
 *   <li>ENDBR    → close cursor / end of query scope</li>
 * </ul>
 *
 * Spring Data Pageable handles all of the above with a single repository method.
 * Sorting is always by {@code tranOrigTs DESC} to mirror the original descending
 * date-ordered display.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionData, String> {

    /**
     * Retrieve a page of transactions for a specific card number (account),
     * optionally filtered by an origination-date range.
     *
     * @param cardNum   card / account identifier (TRAN-CARD-NUM)
     * @param startDate inclusive lower bound for TRAN-ORIG-TS (nullable)
     * @param endDate   inclusive upper bound for TRAN-ORIG-TS (nullable)
     * @param pageable  pagination and sort descriptor (page + size + sort)
     * @return a {@link Page} of matching {@link TransactionData} records
     */
    @Query("""
            SELECT t FROM TransactionData t
             WHERE t.tranCardNum = :cardNum
               AND (:startDate IS NULL OR t.tranOrigTs >= :startDate)
               AND (:endDate   IS NULL OR t.tranOrigTs <= :endDate)
            """)
    Page<TransactionData> findByCardNumAndDateRange(
            @Param("cardNum")    String cardNum,
            @Param("startDate")  LocalDateTime startDate,
            @Param("endDate")    LocalDateTime endDate,
            Pageable pageable);

    /**
     * Retrieve all transactions for a card number (no date filter).
     *
     * @param cardNum  card / account identifier (TRAN-CARD-NUM)
     * @param pageable pagination and sort descriptor
     * @return a {@link Page} of matching {@link TransactionData} records
     */
    Page<TransactionData> findByTranCardNum(String cardNum, Pageable pageable);
}
