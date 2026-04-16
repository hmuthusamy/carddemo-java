package com.carddemo.service;

import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.repository.CardDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Business-logic service for the CBACT02C card-file batch migration.
 *
 * <h2>COBOL Mapping</h2>
 * The original {@code CBACT02C.CBL} performs a sequential READ of the
 * CARDFILE VSAM KSDS (indexed by CARD-NUM) and DISPLAYs each record.
 * In the modernised architecture the READ-and-update pattern is expressed
 * here: each {@link CardUpdateRequest} item (read from a flat file or DB
 * staging table) is validated, looked up by its primary key, and all
 * non-null fields from the request are applied to the persisted entity.
 *
 * <h2>Error Handling (mirrors COBOL ABEND logic)</h2>
 * <ul>
 *   <li>FILE STATUS '00' → {@code APPL-RESULT = 0} → continue (OK)</li>
 *   <li>FILE STATUS '10' → {@code APPL-RESULT = 16} → end-of-file
 *       (handled by batch framework exhaustion)</li>
 *   <li>Any other status → {@code APPL-RESULT = 12} → ABEND →
 *       throw {@link CardFileProcessingException}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cbact02cService {

    private final CardDataRepository cardDataRepository;

    // -----------------------------------------------------------------------
    // Constants mirroring COBOL condition-names
    // -----------------------------------------------------------------------
    private static final int APPL_AOK  = 0;   // 88 APPL-AOK  VALUE 0
    private static final int APPL_EOF  = 16;  // 88 APPL-EOF  VALUE 16
    private static final int APPL_ERR  = 12;  // error / ABEND result

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Process a single {@link CardUpdateRequest}.
     *
     * <p>Logic mirrors the COBOL 1000-CARDFILE-GET-NEXT / REWRITE pattern:
     * <ol>
     *   <li>Look up the card record by CARD-NUM (primary key).</li>
     *   <li>If not found, treat as a new insert (upsert semantics).</li>
     *   <li>Apply field updates following the EVALUATE / IF logic.</li>
     *   <li>Persist and return the updated entity.</li>
     * </ol>
     *
     * @param request update request derived from input file record
     * @return the persisted/updated {@link CardData} entity
     * @throws CardFileProcessingException if mandatory key is absent (ABEND path)
     */
    @Transactional
    public CardData processCardUpdate(CardUpdateRequest request) {
        validateRequest(request);                     // ABEND on bad input

        Optional<CardData> existing =
                cardDataRepository.findById(request.getCardNum());

        CardData card = existing.orElseGet(() -> {
            log.info("CBACT02C – new card record, inserting: cardNum={}",
                     request.getCardNum());
            return CardData.builder()
                           .cardNum(request.getCardNum())
                           .build();
        });

        applyUpdates(card, request);                  // COBOL MOVE / IF logic

        CardData saved = cardDataRepository.save(card);
        log.debug("CBACT02C – saved cardNum={} acctId={}",
                  saved.getCardNum(), saved.getCardAcctId());
        return saved;
    }

    /**
     * Retrieve a card record by card number.
     * Corresponds to a random-access READ of the VSAM KSDS.
     *
     * @param cardNum card number (CARD-NUM PIC X(16))
     * @return the {@link CardData} entity
     * @throws CardFileProcessingException when card not found (APPL-RESULT=12)
     */
    @Transactional(readOnly = true)
    public CardData getCard(String cardNum) {
        return cardDataRepository.findById(cardNum)
                .orElseThrow(() -> new CardFileProcessingException(
                        "ERROR READING CARDFILE – card not found: " + cardNum,
                        APPL_ERR));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Validate that mandatory fields are present.
     * COBOL ABEND path (APPL-RESULT = 12) when CARD-NUM is blank.
     */
    private void validateRequest(CardUpdateRequest request) {
        if (request == null || request.getCardNum() == null
                || request.getCardNum().isBlank()) {
            throw new CardFileProcessingException(
                    "ERROR READING CARDFILE – CARD-NUM is required", APPL_ERR);
        }
    }

    /**
     * Apply field-level updates from the request to the entity.
     *
     * <p>Mirrors the COBOL EVALUATE / IF block that conditionally MOVEs
     * incoming values to the working-storage CARD-RECORD area before REWRITE:
     * <ul>
     *   <li>Non-null / non-blank values overwrite the existing field.</li>
     *   <li>Null values in the request leave the existing field unchanged
     *       (idempotent partial-update semantics).</li>
     *   <li>CARD-ACTIVE-STATUS is constrained to 'Y' or 'N'; any other
     *       value is treated as 'N' (COBOL default / INITIALIZE).</li>
     * </ul>
     */
    private void applyUpdates(CardData card, CardUpdateRequest request) {
        // CARD-ACCT-ID: apply if present
        if (request.getCardAcctId() != null) {
            card.setCardAcctId(request.getCardAcctId());
        }

        // CARD-CVV-CD: apply if present
        if (request.getCardCvvCd() != null) {
            card.setCardCvvCd(request.getCardCvvCd());
        }

        // CARD-EMBOSSED-NAME: apply if non-blank (mirrors COBOL SPACE check)
        if (request.getCardEmbossedName() != null
                && !request.getCardEmbossedName().isBlank()) {
            card.setCardEmbossedName(
                    truncate(request.getCardEmbossedName(), 50));
        }

        // CARD-EXPIRAION-DATE: apply if non-blank
        if (request.getCardExpirationDate() != null
                && !request.getCardExpirationDate().isBlank()) {
            card.setCardExpirationDate(
                    truncate(request.getCardExpirationDate(), 10));
        }

        // CARD-ACTIVE-STATUS: EVALUATE – only 'Y'/'N' are valid values
        if (request.getCardActiveStatus() != null) {
            String status = request.getCardActiveStatus().toUpperCase().trim();
            // EVALUATE CARD-ACTIVE-STATUS
            //   WHEN 'Y'  MOVE 'Y' TO CARD-ACTIVE-STATUS
            //   WHEN 'N'  MOVE 'N' TO CARD-ACTIVE-STATUS
            //   WHEN OTHER MOVE 'N' TO CARD-ACTIVE-STATUS
            card.setCardActiveStatus(
                    "Y".equals(status) ? "Y" : "N");
        }
    }

    /** Truncate a string to maxLen, mirroring fixed-length PIC X(n) fields. */
    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    // -----------------------------------------------------------------------
    // Inner exception class (mirrors COBOL ABEND / 9999-ABEND-PROGRAM)
    // -----------------------------------------------------------------------

    /**
     * Unchecked exception thrown when the card-file processing encounters a
     * fatal error, equivalent to the COBOL {@code 9999-ABEND-PROGRAM} routine.
     */
    public static class CardFileProcessingException extends RuntimeException {
        private final int applResult;

        public CardFileProcessingException(String message, int applResult) {
            super(message);
            this.applResult = applResult;
        }

        /** Returns the COBOL APPL-RESULT code (12 = error, 16 = EOF). */
        public int getApplResult() {
            return applResult;
        }
    }
}
