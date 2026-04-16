package com.carddemo.controller;

import com.carddemo.exception.CardDataStaleException;
import com.carddemo.exception.CardNotFoundException;
import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.model.CardUpdateResponse;
import com.carddemo.service.CardUpdateService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for credit-card update operations.
 *
 * Migrates COBOL/CICS program COCRDUPC (transaction CCUP) to a stateless
 * Spring Boot REST API.  The CICS conversational/pseudo-conversational screen
 * interaction is replaced by standard HTTP verbs.
 *
 * <pre>
 * COBOL paragraph → REST endpoint mapping
 * ─────────────────────────────────────────────────────────────────────
 * 9000/9100-READ-DATA / GETCARD-BYACCTCARD  → GET  /api/cards/{cardNumber}
 * 9200-WRITE-PROCESSING + EXEC CICS REWRITE → PUT  /api/cards/{cardNumber}
 * 1200-EDIT-MAP-INPUTS (all 1210–1260)       → @Valid on CardUpdateRequest
 * 9300-CHECK-CHANGE-IN-REC                  → JPA @Version + 409 CONFLICT
 * COULD-NOT-LOCK-FOR-UPDATE                 → 404 NOT FOUND
 * CONFIRM-UPDATE-SUCCESS                    → 200 OK  + SUCCESS body
 * </pre>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardUpdateController {

    private final CardUpdateService cardUpdateService;

    // -----------------------------------------------------------------------
    // GET /api/cards/{cardNumber}
    // Maps to 9000-READ-DATA / 9100-GETCARD-BYACCTCARD
    // -----------------------------------------------------------------------

    /**
     * Retrieve credit card details by card number.
     *
     * COBOL equivalent:
     *   EXEC CICS READ FILE(CARDDAT)
     *        RIDFLD(WS-CARD-RID-CARDNUM) INTO(CARD-RECORD)
     *
     * @param cardNumber 16-digit numeric card number (CARD-NUM PIC X(16))
     * @return {@code 200 OK} with {@link CardData} body, or {@code 404} if not found
     */
    @GetMapping("/{cardNumber}")
    public ResponseEntity<CardUpdateResponse> getCard(
            @PathVariable
            @Pattern(regexp = "\\d{16}", message = "Card number if supplied must be a 16 digit number")
            String cardNumber) {

        log.info("GET /api/cards/{} — 9100-GETCARD-BYACCTCARD", cardNumber);

        CardData card = cardUpdateService.getCard(cardNumber);

        return ResponseEntity.ok(
                CardUpdateResponse.builder()
                        .status(CardUpdateResponse.Status.SUCCESS)
                        .message("Details of selected card shown above")
                        .card(card)
                        .build());
    }

    // -----------------------------------------------------------------------
    // PUT /api/cards/{cardNumber}
    // Maps to 9200-WRITE-PROCESSING (EXEC CICS REWRITE FILE(CARDDAT))
    // -----------------------------------------------------------------------

    /**
     * Update credit card details.
     *
     * COBOL equivalent (paragraph 9200-WRITE-PROCESSING):
     * <ol>
     *   <li>EXEC CICS READ FILE(CARDDAT) UPDATE</li>
     *   <li>9300-CHECK-CHANGE-IN-REC (stale data check)</li>
     *   <li>EXEC CICS REWRITE FILE(CARDDAT) FROM(CARD-UPDATE-RECORD)</li>
     * </ol>
     *
     * Input validation mirrors paragraphs 1210-EDIT-ACCOUNT through
     * 1260-EDIT-EXPIRY-YEAR via Bean Validation annotations on
     * {@link CardUpdateRequest}.
     *
     * @param cardNumber 16-digit numeric card number (path variable)
     * @param request    fields to update
     * @return {@code 200 OK} on success, {@code 409 CONFLICT} on stale data,
     *         {@code 404} when card not found, {@code 400} on validation errors
     */
    @PutMapping("/{cardNumber}")
    @Transactional
    public ResponseEntity<CardUpdateResponse> updateCard(
            @PathVariable
            @Pattern(regexp = "\\d{16}", message = "Card number if supplied must be a 16 digit number")
            String cardNumber,
            @Valid @RequestBody CardUpdateRequest request) {

        log.info("PUT /api/cards/{} — 9200-WRITE-PROCESSING", cardNumber);

        // Luhn check (extended 1220-EDIT-CARD)
        if (!cardUpdateService.validateLuhn(cardNumber)) {
            log.warn("Luhn check failed for card {}", cardNumber);
            return ResponseEntity.badRequest().body(
                    CardUpdateResponse.builder()
                            .status(CardUpdateResponse.Status.ERROR)
                            .message("Card number failed Luhn integrity check")
                            .build());
        }

        CardUpdateResponse response = cardUpdateService.updateCard(cardNumber, request);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // Exception handlers
    // -----------------------------------------------------------------------

    /**
     * Handles ConstraintViolationException from @Validated path-variable constraints.
     * Returns 400 Bad Request so test TC-03 and TC-19 see the expected status.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CardUpdateResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation on path variable: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                CardUpdateResponse.builder()
                        .status(CardUpdateResponse.Status.ERROR)
                        .message(ex.getMessage())
                        .build());
    }

    /**
     * Handles CardNotFoundException.
     * COBOL: WHEN DFHRESP(NOTFND) → SET DID-NOT-FIND-ACCTCARD-COMBO TO TRUE
     *        WS-RETURN-MSG: 'Did not find cards for this search condition'
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<CardUpdateResponse> handleNotFound(CardNotFoundException ex) {
        log.warn("Card not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                CardUpdateResponse.builder()
                        .status(CardUpdateResponse.Status.NOT_FOUND)
                        .message(ex.getMessage())
                        .build());
    }

    /**
     * Handles CardDataStaleException (optimistic lock / concurrent update).
     * COBOL paragraph 9300-CHECK-CHANGE-IN-REC:
     *   SET DATA-WAS-CHANGED-BEFORE-UPDATE TO TRUE
     *   WS-RETURN-MSG: 'Record changed by some one else. Please review'
     */
    @ExceptionHandler(CardDataStaleException.class)
    public ResponseEntity<CardUpdateResponse> handleConflict(CardDataStaleException ex) {
        log.warn("Stale card data conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                CardUpdateResponse.builder()
                        .status(CardUpdateResponse.Status.CONFLICT)
                        .message(ex.getMessage())
                        .build());
    }
}
