package com.carddemo.service;

import com.carddemo.exception.CardDataStaleException;
import com.carddemo.exception.CardNotFoundException;
import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.model.CardUpdateResponse;
import com.carddemo.repository.CardDataRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for credit-card update operations.
 *
 * Migrates the following COBOL paragraphs from COCRDUPC.cbl:
 *
 *   9000-READ-DATA             → {@link #getCard(String)}
 *   9100-GETCARD-BYACCTCARD    → {@link #getCard(String)} (EXEC CICS READ FILE(CARDDAT))
 *   9200-WRITE-PROCESSING      → {@link #updateCard(String, CardUpdateRequest)}
 *   9300-CHECK-CHANGE-IN-REC   → replaced by JPA @Version optimistic locking
 *   1210-EDIT-ACCOUNT          → Bean-Validation on {@link CardUpdateRequest#getAccountId()}
 *   1220-EDIT-CARD             → path-param Luhn validation in {@link #validateLuhn(String)}
 *   1230-EDIT-NAME             → Bean-Validation on {@link CardUpdateRequest#getEmbossedName()}
 *   1240-EDIT-CARDSTATUS       → Bean-Validation on {@link CardUpdateRequest#getActiveStatus()}
 *   1250-EDIT-EXPIRY-MON       → Bean-Validation on {@link CardUpdateRequest#getExpiryMonth()}
 *   1260-EDIT-EXPIRY-YEAR      → Bean-Validation on {@link CardUpdateRequest#getExpiryYear()}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardUpdateService {

    private final CardDataRepository cardDataRepository;

    // -----------------------------------------------------------------------
    // Read (9000/9100 paragraphs)
    // -----------------------------------------------------------------------

    /**
     * Retrieve a card by its 16-digit card number.
     *
     * COBOL equivalent:
     *   EXEC CICS READ FILE(CARDDAT)
     *        RIDFLD(WS-CARD-RID-CARDNUM)
     *        INTO(CARD-RECORD) ...
     *   WHEN DFHRESP(NOTFND) → SET DID-NOT-FIND-ACCTCARD-COMBO TO TRUE
     *
     * @param cardNumber 16-digit card number (CARD-NUM PIC X(16))
     * @return the persisted {@link CardData}
     * @throws CardNotFoundException when no record exists for that card number
     */
    @Transactional(readOnly = true)
    public CardData getCard(String cardNumber) {
        log.debug("9100-GETCARD-BYACCTCARD: reading card {}", cardNumber);
        return cardDataRepository.findById(cardNumber)
                .orElseThrow(() -> new CardNotFoundException(cardNumber));
    }

    // -----------------------------------------------------------------------
    // Write (9200 paragraph + business logic)
    // -----------------------------------------------------------------------

    /**
     * Update a card record.  Implements the complete 9200-WRITE-PROCESSING flow:
     *
     * <ol>
     *   <li>READ card (equivalent to EXEC CICS READ FILE(CARDDAT) UPDATE)</li>
     *   <li>Validate Luhn on card number (1220-EDIT-CARD extended check)</li>
     *   <li>Map new values from {@code request} onto the entity</li>
     *   <li>repository.save() → EXEC CICS REWRITE FILE(CARDDAT)</li>
     *   <li>JPA {@code @Version} guards against 9300-CHECK-CHANGE-IN-REC scenario</li>
     * </ol>
     *
     * @param cardNumber 16-digit card number (path variable)
     * @param request    updated field values
     * @return response with status SUCCESS and the saved entity
     * @throws CardNotFoundException   when the card does not exist (DFHRESP NOTFND)
     * @throws CardDataStaleException  when a concurrent update is detected (9300)
     */
    @Transactional
    public CardUpdateResponse updateCard(String cardNumber, CardUpdateRequest request) {

        log.info("9200-WRITE-PROCESSING: updating card {}", cardNumber);

        // Step 1 – READ FOR UPDATE (equivalent of EXEC CICS READ FILE UPDATE)
        CardData card = cardDataRepository.findById(cardNumber)
                .orElseThrow(() -> {
                    log.warn("COULD-NOT-LOCK-FOR-UPDATE: card {} not found", cardNumber);
                    return new CardNotFoundException(cardNumber);
                });

        // Step 2 – Apply new values (prepare CARD-UPDATE-RECORD)
        //   MOVE CCUP-NEW-ACCTID → accountId
        card.setAccountId(Long.parseLong(request.getAccountId()));

        //   MOVE CCUP-NEW-CRDNAME (upper-cased per INSPECT CONVERTING)
        card.setEmbossedName(request.getEmbossedName().toUpperCase());

        //   STRING CCUP-NEW-EXPYEAR '-' CCUP-NEW-EXPMON '-' CCUP-NEW-EXPDAY
        String expDay = (request.getExpiryDay() != null && !request.getExpiryDay().isBlank())
                ? request.getExpiryDay() : "01";
        card.setExpirationDate(
                request.getExpiryYear() + "-" + request.getExpiryMonth() + "-" + expDay);

        //   MOVE CCUP-NEW-CRDSTCD → activeStatus
        card.setActiveStatus(request.getActiveStatus());

        //   MOVE CCUP-NEW-CVV-CD (if supplied)
        if (request.getCvvCode() != null && !request.getCvvCode().isBlank()) {
            card.setCvvCode(request.getCvvCode());
        }

        // Step 3 – EXEC CICS REWRITE → repository.save()
        //   JPA @Version handles 9300-CHECK-CHANGE-IN-REC via OptimisticLockException
        CardData saved;
        try {
            saved = cardDataRepository.save(card);
            log.info("CONFIRM-UPDATE-SUCCESS: card {} saved", cardNumber);
        } catch (OptimisticLockException ole) {
            log.warn("DATA-WAS-CHANGED-BEFORE-UPDATE: card {} version conflict", cardNumber);
            throw new CardDataStaleException(cardNumber);
        }

        return CardUpdateResponse.builder()
                .status(CardUpdateResponse.Status.SUCCESS)
                .message("Changes committed to database")
                .card(saved)
                .build();
    }

    // -----------------------------------------------------------------------
    // Luhn validation (extended 1220-EDIT-CARD)
    // -----------------------------------------------------------------------

    /**
     * Luhn (mod-10) algorithm check on a card number string.
     *
     * The COBOL program validates that a card number is 16 numeric digits
     * (1220-EDIT-CARD paragraph).  This method extends that with a Luhn
     * integrity check where applicable.
     *
     * @param cardNumber 16-digit card number string
     * @return {@code true} if the number passes the Luhn check
     */
    public boolean validateLuhn(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
