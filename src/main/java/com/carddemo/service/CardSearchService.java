package com.carddemo.service;

import com.carddemo.model.CardSearchRequest;
import com.carddemo.model.CreditCard;
import com.carddemo.repository.CreditCardRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business-logic service for credit-card search operations.
 *
 * <p>Directly migrated from COBOL program COCRDSLC (CICS program COCRDSLW).
 * The table below maps each COBOL paragraph to its Java equivalent:</p>
 *
 * <pre>
 * COBOL paragraph / section            Java method / responsibility
 * -----------------------------------  -------------------------------------------
 * 2210-EDIT-ACCOUNT                    validateRequest() – account-ID validation
 * 2220-EDIT-CARD                       validateRequest() – card-number validation
 * Cross-field edit (2200-EDIT-MAP)     validateRequest() – at-least-one-criteria
 * 9000-READ-DATA                       searchCards()
 * 9100-GETCARD-BYACCTCARD              buildSpecification() – primary-key lookup
 * 9150-GETCARD-BYACCT                  buildSpecification() – alternate-index lookup
 * </pre>
 *
 * <p>COBOL error messages are preserved as constants and surfaced via
 * {@link CardSearchException}.</p>
 */
@Service
public class CardSearchService {

    // -------------------------------------------------------------------------
    // COBOL error-message constants (from WS-RETURN-MSG 88-level values)
    // -------------------------------------------------------------------------
    public static final String MSG_NO_CRITERIA    =
            "No input received";
    public static final String MSG_ACCT_NOT_VALID =
            "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER";
    public static final String MSG_CARD_NOT_VALID =
            "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER";
    public static final String MSG_NOT_FOUND      =
            "Did not find cards for this search condition";

    private final CreditCardRepository creditCardRepository;

    @Autowired
    public CardSearchService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Entry point: validate inputs, build dynamic query, execute search.
     *
     * <p>Mirrors the COBOL PROCEDURE DIVISION flow:
     * 2000-PROCESS-INPUTS → 9000-READ-DATA.</p>
     *
     * @param req  search criteria (may be partially populated)
     * @return     list of matching {@link CreditCard} records
     * @throws CardSearchException for invalid or missing input
     */
    public List<CreditCard> searchCards(CardSearchRequest req) {
        // Paragraph 2000-PROCESS-INPUTS / 2200-EDIT-MAP-INPUTS
        validateRequest(req);

        // Paragraph 9000-READ-DATA → 9100 / 9150 via JPA Specification
        Specification<CreditCard> spec = buildSpecification(req);
        List<CreditCard> results = creditCardRepository.findAll(spec);

        if (results.isEmpty()) {
            throw new CardSearchException(MSG_NOT_FOUND);
        }
        return results;
    }

    /**
     * Fetch a single card by its primary key (16-digit card number).
     *
     * <p>Mirrors COBOL paragraph 9100-GETCARD-BYACCTCARD:
     * {@code EXEC CICS READ FILE(CARDDAT) RIDFLD(WS-CARD-RID-CARDNUM)}.</p>
     *
     * @param cardNumber 16-digit card number
     * @return the matching card
     * @throws CardSearchException if not found
     */
    public CreditCard getCardByNumber(String cardNumber) {
        validateCardNumber(cardNumber);
        return creditCardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CardSearchException(MSG_NOT_FOUND));
    }

    /**
     * Fetch all cards linked to an account (alternate-index path).
     *
     * <p>Mirrors COBOL paragraph 9150-GETCARD-BYACCT:
     * {@code EXEC CICS READ FILE(CARDAIX) RIDFLD(WS-CARD-RID-ACCT-ID)}.</p>
     *
     * @param accountId 11-digit account identifier
     * @return list of cards belonging to the account
     * @throws CardSearchException if not found
     */
    public List<CreditCard> getCardsByAccountId(Long accountId) {
        validateAccountId(accountId);
        List<CreditCard> results = creditCardRepository.findByAccountId(accountId);
        if (results.isEmpty()) {
            throw new CardSearchException(MSG_NOT_FOUND);
        }
        return results;
    }

    // =========================================================================
    // Validation helpers (COBOL paragraphs 2210-EDIT-ACCOUNT / 2220-EDIT-CARD)
    // =========================================================================

    /**
     * Validates the search request.
     *
     * <p>Rules migrated from COBOL:
     * <ul>
     *   <li>2210: accountId, if provided, must be numeric ≤ 11 digits and non-zero</li>
     *   <li>2220: cardNumber, if provided, must be exactly 16 digits and non-zero</li>
     *   <li>Cross-field: at least one criterion required (NO-SEARCH-CRITERIA-RECEIVED)</li>
     * </ul>
     * </p>
     */
    void validateRequest(CardSearchRequest req) {
        boolean hasAccount = req.getAccountId() != null && req.getAccountId() != 0L;
        boolean hasCard    = req.getCardNumber() != null
                          && !req.getCardNumber().isBlank()
                          && !req.getCardNumber().equals("0000000000000000");

        // Cross-field edit: NO-SEARCH-CRITERIA-RECEIVED
        if (!hasAccount && !hasCard) {
            throw new CardSearchException(MSG_NO_CRITERIA);
        }

        if (req.getAccountId() != null && req.getAccountId() != 0L) {
            validateAccountId(req.getAccountId());
        }

        if (req.getCardNumber() != null && !req.getCardNumber().isBlank()) {
            validateCardNumber(req.getCardNumber());
        }
    }

    /**
     * Validates the account ID.
     * COBOL: account must be a non-zero 11-digit number (2210-EDIT-ACCOUNT).
     */
    void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new CardSearchException(MSG_ACCT_NOT_VALID);
        }
        String s = String.valueOf(accountId);
        if (s.length() > 11 || !s.matches("\\d+")) {
            throw new CardSearchException(MSG_ACCT_NOT_VALID);
        }
    }

    /**
     * Validates the card number.
     * COBOL: card number must be a 16-digit numeric non-zero value (2220-EDIT-CARD).
     */
    void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new CardSearchException(MSG_CARD_NOT_VALID);
        }
        String trimmed = cardNumber.trim();
        if (trimmed.length() != 16 || !trimmed.matches("\\d{16}")) {
            throw new CardSearchException(MSG_CARD_NOT_VALID);
        }
        if (trimmed.equals("0000000000000000")) {
            throw new CardSearchException(MSG_CARD_NOT_VALID);
        }
    }

    // =========================================================================
    // JPA Specification builder
    // =========================================================================

    /**
     * Builds a JPA {@link Specification} from the validated search criteria.
     *
     * <p>Maps to both COBOL data-access paragraphs:
     * <ul>
     *   <li>cardNumber supplied → 9100-GETCARD-BYACCTCARD (primary key read)</li>
     *   <li>accountId supplied  → 9150-GETCARD-BYACCT (alternate index)</li>
     *   <li>status supplied     → additional predicate on CARD-ACTIVE-STATUS</li>
     * </ul>
     * Predicates are AND-combined, preserving COBOL search semantics.</p>
     */
    Specification<CreditCard> buildSpecification(CardSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 9100-GETCARD-BYACCTCARD: search by card number (primary key / CARDDAT)
            if (req.getCardNumber() != null && !req.getCardNumber().isBlank()) {
                predicates.add(cb.equal(root.get("cardNumber"),
                        req.getCardNumber().trim()));
            }

            // 9150-GETCARD-BYACCT: search by account ID (alternate index / CARDAIX)
            if (req.getAccountId() != null && req.getAccountId() != 0L) {
                predicates.add(cb.equal(root.get("accountId"), req.getAccountId()));
            }

            // CARD-ACTIVE-STATUS filter (CARD-ACTIVE-STATUS PIC X(1))
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("activeStatus"),
                        req.getStatus().trim().toUpperCase()));
            }

            // ORDER BY card_number ASC – deterministic ordering for UI pagination
            query.orderBy(cb.asc(root.get("cardNumber")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================================================================
    // Domain exception
    // =========================================================================

    /**
     * Unchecked exception carrying a COBOL-equivalent error message.
     * Surfaced by the controller as HTTP 4xx / 5xx responses.
     */
    public static class CardSearchException extends RuntimeException {
        public CardSearchException(String message) {
            super(message);
        }
    }
}
