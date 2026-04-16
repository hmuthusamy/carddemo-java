package com.carddemo.controller;

import com.carddemo.model.CardSearchRequest;
import com.carddemo.model.CreditCard;
import com.carddemo.service.CardSearchService;
import com.carddemo.service.CardSearchService.CardSearchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for credit-card search operations.
 *
 * <p>Migrated from COBOL/CICS program <strong>COCRDSLC</strong>
 * (transaction CCDL, mapset COCRDSL, screen CCRDSLA).
 * The interactive CICS screen is replaced by a RESTful HTTP interface.</p>
 *
 * <pre>
 * COBOL interaction flow          →  HTTP equivalent
 * ─────────────────────────────── ─────────────────────────────────────────
 * 1000-SEND-MAP (initial screen)  GET /api/cards/search (no params)  → 400
 * 2000-PROCESS-INPUTS             Request parameter binding + validation
 * 2210-EDIT-ACCOUNT               Validated inside CardSearchService
 * 2220-EDIT-CARD                  Validated inside CardSearchService
 * 9000-READ-DATA                  CardSearchService.searchCards()
 * 9100-GETCARD-BYACCTCARD         GET /api/cards/{cardNumber}
 * 9150-GETCARD-BYACCT             GET /api/cards/account/{accountId}
 * </pre>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/cards/search}        – dynamic multi-criteria search</li>
 *   <li>{@code GET /api/cards/{cardNumber}}  – lookup by 16-digit card number</li>
 *   <li>{@code GET /api/cards/account/{id}}  – lookup all cards for an account</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cards")
public class CardSearchController {

    private final CardSearchService cardSearchService;

    @Autowired
    public CardSearchController(CardSearchService cardSearchService) {
        this.cardSearchService = cardSearchService;
    }

    // =========================================================================
    // GET /api/cards/search
    // =========================================================================

    /**
     * Dynamic credit-card search with optional query parameters.
     *
     * <p>Preserves all COBOL search conditions from
     * {@code 2200-EDIT-MAP-INPUTS}, {@code 9100-GETCARD-BYACCTCARD},
     * and {@code 9150-GETCARD-BYACCT}:</p>
     * <ul>
     *   <li>At least one of {@code cardNumber} or {@code customerId} is required</li>
     *   <li>{@code cardNumber} must be exactly 16 numeric digits (non-zero)</li>
     *   <li>{@code customerId} must be a non-zero number ≤ 11 digits</li>
     *   <li>{@code status} filters on {@code CARD-ACTIVE-STATUS} (Y / N)</li>
     * </ul>
     *
     * @param cardNumber   16-digit card number (maps to CC-CARD-NUM / CDEMO-CARD-NUM)
     * @param customerId   11-digit account/customer ID (maps to CC-ACCT-ID / CDEMO-ACCT-ID)
     * @param status       Card active status: 'Y' or 'N' (maps to CARD-ACTIVE-STATUS)
     * @return             200 + list of matching cards, or 4xx on validation error
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCards(
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) Long   customerId,
            @RequestParam(required = false) String status) {

        CardSearchRequest req = new CardSearchRequest(customerId, cardNumber, status);

        try {
            List<CreditCard> results = cardSearchService.searchCards(req);
            return ResponseEntity.ok(results);
        } catch (CardSearchException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/cards/{cardNumber}
    // =========================================================================

    /**
     * Fetch a single card by its 16-digit primary-key card number.
     *
     * <p>Migrated from COBOL paragraph {@code 9100-GETCARD-BYACCTCARD}:
     * {@code EXEC CICS READ FILE(CARDDAT) RIDFLD(WS-CARD-RID-CARDNUM)}.</p>
     *
     * @param cardNumber 16-digit card number
     * @return 200 + card record, 400 on invalid input, 404 if not found
     */
    @GetMapping("/{cardNumber}")
    public ResponseEntity<?> getCardByNumber(@PathVariable String cardNumber) {
        try {
            CreditCard card = cardSearchService.getCardByNumber(cardNumber);
            return ResponseEntity.ok(card);
        } catch (CardSearchException ex) {
            // Distinguish "not found" from "bad input" for proper HTTP semantics
            String msg = ex.getMessage();
            if (CardSearchService.MSG_NOT_FOUND.equals(msg)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // =========================================================================
    // GET /api/cards/account/{accountId}
    // =========================================================================

    /**
     * Fetch all cards belonging to an account (alternate-index lookup).
     *
     * <p>Migrated from COBOL paragraph {@code 9150-GETCARD-BYACCT}:
     * {@code EXEC CICS READ FILE(CARDAIX) RIDFLD(WS-CARD-RID-ACCT-ID)}.</p>
     *
     * @param accountId 11-digit account identifier
     * @return 200 + list of cards, 400 on invalid input, 404 if none found
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getCardsByAccount(@PathVariable Long accountId) {
        try {
            List<CreditCard> cards = cardSearchService.getCardsByAccountId(accountId);
            return ResponseEntity.ok(cards);
        } catch (CardSearchException ex) {
            String msg = ex.getMessage();
            if (CardSearchService.MSG_NOT_FOUND.equals(msg)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }
}
