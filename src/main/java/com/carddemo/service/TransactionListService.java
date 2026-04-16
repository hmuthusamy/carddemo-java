package com.carddemo.service;

import com.carddemo.model.TransactionData;
import com.carddemo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service layer for the Transaction List feature.
 *
 * <p><b>COBOL ↔ Java mapping (COTRN00C / COTRNLIC):</b>
 * <table border="1">
 *   <tr><th>COBOL concept</th><th>Java equivalent</th></tr>
 *   <tr><td>EXEC CICS STARTBR DATASET('TRANSACT') RIDFLD(TRAN-ID)</td>
 *       <td>{@link TransactionRepository#findByCardNumAndDateRange} opens a DB cursor via JPA</td></tr>
 *   <tr><td>EXEC CICS READNEXT (page-forward loop, up to 10 rows)</td>
 *       <td>Spring Data {@link Pageable} with {@code page} and {@code size}</td></tr>
 *   <tr><td>EXEC CICS READPREV (page-backward loop)</td>
 *       <td>Decrement page number, same {@link Pageable} abstraction</td></tr>
 *   <tr><td>EXEC CICS ENDBR</td>
 *       <td>Implicit – connection returned to pool after transaction</td></tr>
 *   <tr><td>WS-PAGE-NUM, CDEMO-CT00-NEXT-PAGE-FLG</td>
 *       <td>{@link Page#getNumber()}, {@link Page#hasNext()}, {@link Page#hasPrevious()}</td></tr>
 *   <tr><td>TRAN-ORIG-TS descending screen order</td>
 *       <td>{@code Sort.by("tranOrigTs").descending()}</td></tr>
 * </table>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionListService {

    /** Default page size matches the COBOL screen (10 rows). */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final TransactionRepository transactionRepository;

    /**
     * Retrieve a paginated list of transactions for a given account / card number.
     *
     * <p>Sort order is always {@code tranOrigTs DESC}, preserving the original
     * CICS VSAM browse behaviour where most-recent transactions appear first.
     *
     * @param accountId  card number (maps to TRAN-CARD-NUM / TRNIDINI field)
     * @param page       zero-based page index (CDEMO-CT00-PAGE-NUM - 1)
     * @param size       number of records per page (default 10, COBOL screen limit)
     * @param startDate  optional inclusive start of TRAN-ORIG-TS range (date-range filter)
     * @param endDate    optional inclusive end   of TRAN-ORIG-TS range
     * @return {@link Page} of {@link TransactionData} records
     * @throws IllegalArgumentException when {@code accountId} is blank
     */
    @Transactional(readOnly = true)
    public Page<TransactionData> listTransactions(String accountId,
                                                   int page,
                                                   int size,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page < 0) {
            page = 0;
        }

        // Always sort descending by origination timestamp
        // – mirrors COBOL READNEXT ordered by TRAN-ID (time-stamped key desc)
        Sort sort = Sort.by(Sort.Direction.DESC, "tranOrigTs");
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime startTs = (startDate != null)
                ? startDate.atStartOfDay()
                : null;
        LocalDateTime endTs = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : null;

        Page<TransactionData> result;
        if (startTs != null || endTs != null) {
            log.debug("Querying transactions: accountId={}, page={}, size={}, start={}, end={}",
                    accountId, page, size, startTs, endTs);
            result = transactionRepository.findByCardNumAndDateRange(
                    accountId, startTs, endTs, pageable);
        } else {
            log.debug("Querying transactions (no date filter): accountId={}, page={}, size={}",
                    accountId, page, size);
            result = transactionRepository.findByTranCardNum(accountId, pageable);
        }

        log.info("Transaction list: accountId={} page={}/{} totalElements={}",
                accountId, result.getNumber(), result.getTotalPages(), result.getTotalElements());
        return result;
    }
}
