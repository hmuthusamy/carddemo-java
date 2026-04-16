package com.carddemo.controller;

import com.carddemo.model.AccountUpdateRequest;
import com.carddemo.model.AccountUpdateResponse;
import com.carddemo.service.AccountUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AccountUpdateController – Spring Boot REST controller.
 *
 * Migrated from COBOL/CICS program COACTUPD.cbl (Account Update screen).
 *
 * <pre>
 * COBOL / CICS → Spring REST mapping
 * ─────────────────────────────────────────────────────────────────────────
 * EXEC CICS RECEIVE MAP('CACTUPAI')   → PUT /api/accounts/{accountId}
 * WS-RETURN-TRANID = 'CA1A'           → @RequestMapping("/api/accounts")
 * EXEC CICS REWRITE FILE('ACCTDAT')   → AccountUpdateService.updateAccount()
 * EXEC CICS SEND MAP('CACTUPAO')      → ResponseEntity<AccountUpdateResponse>
 * RESP = DFHRESP(NOTFND)              → HTTP 404
 * RESP = DFHRESP(NORMAL)              → HTTP 200
 * Validation errors (WS-ERRMSG)       → HTTP 400 + error list
 * Unexpected CICS ABEND               → HTTP 500
 * ─────────────────────────────────────────────────────────────────────────
 * </pre>
 *
 * <b>Endpoint:</b>
 * <pre>
 * PUT /api/accounts/{accountId}
 * Content-Type: application/json
 *
 * {
 *   "activeStatus":    "Y",
 *   "creditLimit":     5000.00,
 *   "cashCreditLimit": 1000.00,
 *   "expirationDate":  "2027-12-31",
 *   "reissueDate":     "2025-01-01",
 *   "addrZip":         "10001",
 *   "groupId":         "GOLD",
 *   "currCycCredit":   200.00,
 *   "currCycDebit":    150.00
 * }
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountUpdateController {

    private final AccountUpdateService accountUpdateService;

    /**
     * Update an existing account record.
     *
     * <ul>
     *   <li>HTTP 200 – account updated successfully (COBOL DFHRESP(NORMAL))</li>
     *   <li>HTTP 400 – validation failure (COBOL 8000-VALIDATE-INPUT error)</li>
     *   <li>HTTP 404 – account not found (COBOL DFHRESP(NOTFND))</li>
     *   <li>HTTP 500 – unexpected server error (COBOL ABEND / CICS error)</li>
     * </ul>
     *
     * @param accountId path variable – numeric account identifier (PIC 9(11))
     * @param request   JSON body with fields to update
     * @return {@link AccountUpdateResponse} wrapped in a {@link ResponseEntity}
     */
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountUpdateResponse> updateAccount(
            @PathVariable("accountId") String accountId,
            @RequestBody AccountUpdateRequest request) {

        log.info("PUT /api/accounts/{} received", accountId);

        try {
            AccountUpdateResponse response = accountUpdateService.updateAccount(accountId, request);

            if (response.isSuccess()) {
                // COBOL: MOVE 'UPDATE SUCCESSFUL' TO WS-RETURN-MSG
                //        EXEC CICS SEND MAP('CACTUPAO') → HTTP 200
                return ResponseEntity.ok(response);
            }

            // Distinguish 404 from 400 by checking whether the message says "not found"
            if (response.getMessage() != null
                    && response.getMessage().toLowerCase().contains("not found")) {
                // COBOL: EXEC CICS HANDLE CONDITION NOTFND → HTTP 404
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // COBOL: WS-ERRMSG populated → HTTP 400 with error detail
            return ResponseEntity.badRequest().body(response);

        } catch (Exception ex) {
            // COBOL: EXEC CICS ABEND ABCODE('CAUE') → HTTP 500
            log.error("Unexpected error updating account {}: {}", accountId, ex.getMessage(), ex);
            AccountUpdateResponse errorResponse = AccountUpdateResponse.error(
                    "Internal server error while updating account: " + accountId, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
