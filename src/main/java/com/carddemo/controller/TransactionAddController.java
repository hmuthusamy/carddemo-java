package com.carddemo.controller;

import com.carddemo.model.TransactionAddRequest;
import com.carddemo.model.TransactionAddResponse;
import com.carddemo.service.TransactionAddService;
import com.carddemo.service.TransactionAddService.TransactionValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TransactionAddController – Spring Boot REST controller.
 *
 * <p>Migrated from COBOL/CICS program <strong>COTRNADD / COTRN02C.CBL</strong>
 * (CardDemo Add Transaction screen, transaction code CT02).
 *
 * <h3>COBOL → REST mapping</h3>
 * <pre>
 * COBOL construct                           Java equivalent
 * ─────────────────────────────────────────────────────────────────────
 * CICS transaction CT02                  →  POST /api/transactions
 * EXEC CICS RECEIVE MAP('COTRN2A')       →  @RequestBody TransactionAddRequest
 * PROCESS-ENTER-KEY / ADD-TRANSACTION    →  transactionAddService.addTransaction()
 * EXEC CICS WRITE DATASET('TRANSACT')    →  transactionRepository.save()  (in service)
 * SEND-TRNADD-SCREEN (success / error)   →  ResponseEntity (201 / 400 / 500)
 * WS-ERR-FLG = 'Y' + SEND-TRNADD-SCREEN →  400 Bad Request + error body
 * </pre>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/transactions} – add a new transaction (201 Created)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionAddController {

    private final TransactionAddService transactionAddService;

    // ================================================================== POST /api/transactions

    /**
     * Adds a new transaction record.
     *
     * <p>Mirrors the full COBOL PROCESS-ENTER-KEY flow:
     * <ol>
     *   <li>Key-field validation (account / card cross-reference lookup)</li>
     *   <li>Data-field validation (type, category, amount, dates, merchant)</li>
     *   <li>Transaction-ID generation (STARTBR HIGH-VALUES + READPREV + ADD 1)</li>
     *   <li>Record write (EXEC CICS WRITE → repository.save())</li>
     * </ol>
     *
     * @param request the transaction data to persist
     * @return 201 Created with the created {@link TransactionAddResponse},
     *         or 400 Bad Request if validation fails
     */
    @PostMapping
    @Transactional
    public ResponseEntity<TransactionAddResponse> addTransaction(
            @RequestBody TransactionAddRequest request) {

        log.info("POST /api/transactions – account={} card={}",
                request.getAccountId(), request.getCardNumber());

        TransactionAddResponse response = transactionAddService.addTransaction(request);

        log.info("Transaction created: tranId={}", response.getTransactionId());
        // HTTP 201 Created – mirrors COBOL "Transaction added successfully." screen message
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================== exception handlers

    /**
     * Handles validation failures raised inside {@link TransactionAddService}.
     *
     * <p>COBOL equivalent: setting WS-ERR-FLG = 'Y' then PERFORM SEND-TRNADD-SCREEN,
     * which re-renders the COTRN2A BMS map with the error message in ERRMSGO.
     */
    @ExceptionHandler(TransactionValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            TransactionValidationException ex) {

        log.warn("Validation error: {}", ex.getMessage());
        // Returns {"error": "<COBOL WS-MESSAGE text>"}
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles unexpected system errors.
     *
     * <p>COBOL equivalent: DFHRESP(OTHER) branch → "Unable to Add Transaction..." message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {

        log.error("Unexpected error in COTRNADD: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to Add Transaction..."));
    }
}
