package com.carddemo.controller;

import com.carddemo.exception.AccountNotFoundException;
import com.carddemo.model.AccountViewResponse;
import com.carddemo.service.AccountViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AccountViewController – Spring Boot REST controller migrated from COBOL/CICS
 * program COACTVWC (transaction id CAVW, BMS mapset COACTVW, map CACTVWA).
 *
 * <p>The original program presented account details on a 3270 BMS screen.
 * This controller replaces that interaction model with a RESTful JSON API.
 *
 * <pre>
 * COBOL flow → Java equivalent
 * ─────────────────────────────────────────────────────────────────────────────
 * EVALUATE TRUE
 *   WHEN CDEMO-PGM-REENTER               →  GET /api/accounts/{accountId}
 *     PERFORM 2000-PROCESS-INPUTS            service.getAccountView(id)
 *     IF INPUT-ERROR                         → 400 Bad Request
 *        PERFORM 1000-SEND-MAP               → ResponseEntity with error body
 *     ELSE
 *        PERFORM 9000-READ-ACCT
 *          IF NOT-FOUND                      → 404 Not Found
 *             PERFORM 1000-SEND-MAP          → ResponseEntity with error body
 *          ELSE
 *             PERFORM 1000-SEND-MAP          → 200 OK with AccountViewResponse
 * </pre>
 *
 * Endpoint:  GET /api/accounts/{accountId}
 * Success:   HTTP 200  – JSON body containing {@link AccountViewResponse}
 * Not Found: HTTP 404  – JSON error body
 * Bad Input: HTTP 400  – JSON error body
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountViewController {

    private static final Logger log = LoggerFactory.getLogger(AccountViewController.class);

    private final AccountViewService accountViewService;

    public AccountViewController(AccountViewService accountViewService) {
        this.accountViewService = accountViewService;
    }

    /**
     * Retrieve account details by account id.
     *
     * <p>Replaces the entire COBOL COACTVWC screen-display flow with a single
     * HTTP GET. The response JSON fields mirror the BMS screen fields defined in
     * mapset COACTVW / map CACTVWA.
     *
     * @param accountId 11-digit account identifier (maps to CDEMO-ACCT-ID / ACCTSIDI)
     * @return 200 OK with {@link AccountViewResponse} body,
     *         or 404 Not Found if the account does not exist,
     *         or 400 Bad Request if the account id is invalid
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountView(@PathVariable Long accountId) {

        log.info("GET /api/accounts/{} – account view requested", accountId);

        try {
            AccountViewResponse response = accountViewService.getAccountView(accountId);
            log.info("GET /api/accounts/{} – returning account data", accountId);
            return ResponseEntity.ok(response);

        } catch (AccountNotFoundException ex) {
            // COBOL: DID-NOT-FIND-ACCT-IN-CARDXREF / DID-NOT-FIND-ACCT-IN-ACCTDAT
            //         / DID-NOT-FIND-CUST-IN-CUSTDAT  → WS-RETURN-MSG set, screen redisplayed
            log.warn("GET /api/accounts/{} – not found: {}", accountId, ex.getMessage());
            return ResponseEntity
                    .status(404)
                    .body(Map.of(
                            "error",     "Account not found",
                            "accountId", accountId,
                            "message",   ex.getMessage()
                    ));

        } catch (IllegalArgumentException ex) {
            // COBOL: SEARCHED-ACCT-ZEROES / SEARCHED-ACCT-NOT-NUMERIC
            //         → INPUT-ERROR set, WS-RETURN-MSG populated, screen redisplayed
            log.warn("GET /api/accounts/{} – invalid input: {}", accountId, ex.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",     "Invalid account id",
                            "accountId", accountId,
                            "message",   ex.getMessage()
                    ));
        }
    }
}
