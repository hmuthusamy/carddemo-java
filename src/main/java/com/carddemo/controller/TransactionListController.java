package com.carddemo.controller;

import com.carddemo.model.TransactionData;
import com.carddemo.service.TransactionListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for the Transaction List screen.
 *
 * <h2>COBOL Origin: COTRN00C.CBL (COTRNLIC – Transaction List CICS program)</h2>
 * <p>The original CICS program displayed a paginated, terminal-based list of
 * transactions for a card account.  This controller replaces that program with
 * a stateless JSON REST API.
 *
 * <h3>CICS → Spring Boot mapping</h3>
 * <pre>
 * ┌──────────────────────────────────┬──────────────────────────────────────────────────┐
 * │ COBOL / CICS concept             │ Spring Boot equivalent                           │
 * ├──────────────────────────────────┼──────────────────────────────────────────────────┤
 * │ EXEC CICS STARTBR DATASET(       │ JPA repository query – DB cursor opened per      │
 * │   'TRANSACT') RIDFLD(TRAN-ID)    │ HTTP request                                     │
 * │ EXEC CICS READNEXT (fwd loop)    │ Pageable page=N, size=S → page N content         │
 * │ EXEC CICS READPREV (bwd loop)    │ Pageable page=N-1 (caller decrements page index) │
 * │ EXEC CICS ENDBR                  │ Implicit on DB connection return                 │
 * │ WS-PAGE-NUM (CDEMO-CT00-PAGE-NUM)│ Page.getNumber() + 1 (1-based for UI parity)    │
 * │ CDEMO-CT00-NEXT-PAGE-FLG         │ Page.hasNext()                                   │
 * │ TRNID filter (TRNIDINI)          │ accountId query param → tranCardNum filter        │
 * │ TRAN-ORIG-TS display order       │ Sort.by("tranOrigTs").descending()               │
 * │ PF7 (page back) / PF8 (page fwd) │ page=N-1 / page=N+1 query param                 │
 * │ Screen field PAGENUMI            │ JSON "pageNumber" in response                    │
 * └──────────────────────────────────┴──────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Endpoint</h3>
 * <pre>
 * GET /api/transactions?accountId={id}&amp;page={n}&amp;size={s}
 *                       [&amp;startDate=YYYY-MM-DD][&amp;endDate=YYYY-MM-DD]
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionListController {

    private final TransactionListService transactionListService;

    /**
     * List transactions for an account, with optional date-range filtering.
     *
     * <p>Parameters mirror the CICS screen fields and PF-key navigation:
     * <ul>
     *   <li>{@code accountId} – replaces TRNIDINI / TRAN-CARD-NUM filter</li>
     *   <li>{@code page}      – zero-based page index; PF7 → page-1, PF8 → page+1</li>
     *   <li>{@code size}      – records per page (COBOL default: 10)</li>
     *   <li>{@code startDate} – replaces implicit start-of-file / READNEXT lower bound</li>
     *   <li>{@code endDate}   – replaces end-of-file / ENDBR upper bound</li>
     * </ul>
     *
     * @param accountId card/account identifier (required)
     * @param page      zero-based page index (default 0)
     * @param size      page size              (default 10)
     * @param startDate optional start of date range (ISO 8601 date, e.g. 2023-01-01)
     * @param endDate   optional end   of date range (ISO 8601 date, e.g. 2023-12-31)
     * @return HTTP 200 with {@link Page} of {@link TransactionData} as JSON,
     *         or HTTP 400 if accountId is blank
     */
    @GetMapping
    public ResponseEntity<Page<TransactionData>> listTransactions(
            @RequestParam                                          String    accountId,
            @RequestParam(defaultValue = "0")                     int       page,
            @RequestParam(defaultValue = "10")                    int       size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)         LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)         LocalDate endDate) {

        log.info("GET /api/transactions accountId={} page={} size={} startDate={} endDate={}",
                accountId, page, size, startDate, endDate);

        if (accountId == null || accountId.isBlank()) {
            log.warn("Request rejected: accountId is blank");
            return ResponseEntity.badRequest().build();
        }

        Page<TransactionData> result =
                transactionListService.listTransactions(accountId, page, size, startDate, endDate);

        return ResponseEntity.ok(result);
    }
}
