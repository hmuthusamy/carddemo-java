package com.carddemo.service;

import com.carddemo.model.CardXref;
import com.carddemo.model.TransactionAddRequest;
import com.carddemo.model.TransactionAddResponse;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * TransactionAddService – business logic layer for adding a new transaction.
 *
 * <p>Direct migration of COBOL program COTRNADD / COTRN02C paragraphs:
 * <pre>
 *   VALIDATE-INPUT-KEY-FIELDS   → resolveAndValidateKeys()
 *   VALIDATE-INPUT-DATA-FIELDS  → validateDataFields()
 *   ADD-TRANSACTION             → buildAndPersistTransaction()
 *   READ-CXACAIX-FILE           → cardXrefRepository.findByXrefAcctId()
 *   READ-CCXREF-FILE            → cardXrefRepository.findByXrefCardNum()
 *   STARTBR + READPREV + ADD 1  → generateNextTransactionId()
 *   EXEC CICS WRITE             → transactionRepository.save()
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAddService {

    // ------------------------------------------------------------------ COBOL constants
    /** TRAN-ID column width – PIC X(16). */
    private static final int TRAN_ID_LENGTH = 16;
    /** Date format used in TORIGDTI / TPROCDTI – YYYY-MM-DD. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ------------------------------------------------------------------ dependencies
    private final TransactionRepository transactionRepository;
    private final CardXrefRepository    cardXrefRepository;

    // ================================================================== public API

    /**
     * Adds a new transaction record.
     *
     * <p>Orchestrates the same sequence as COBOL PROCESS-ENTER-KEY:
     * <ol>
     *   <li>Validate / resolve key fields (account id ↔ card number)</li>
     *   <li>Validate all data fields</li>
     *   <li>Generate the next sequential transaction ID</li>
     *   <li>Persist the record (EXEC CICS WRITE → repository.save())</li>
     * </ol>
     *
     * @param request validated request DTO
     * @return response DTO containing the created transaction
     * @throws TransactionValidationException for any validation failure
     */
    @Transactional
    public TransactionAddResponse addTransaction(TransactionAddRequest request) {

        log.info("COTRNADD: starting ADD-TRANSACTION");

        // ---- VALIDATE-INPUT-KEY-FIELDS --------------------------------
        String[] resolved = resolveAndValidateKeys(request);
        String resolvedCardNum = resolved[0];
        String resolvedAcctId  = resolved[1];

        // ---- VALIDATE-INPUT-DATA-FIELDS --------------------------------
        validateDataFields(request);

        // ---- ADD-TRANSACTION ------------------------------------------
        String newTranId = generateNextTransactionId();

        TransactionData record = TransactionData.builder()
                .tranId(newTranId)
                .tranTypeCd(request.getTypeCode().trim())
                .tranCatCd(request.getCategoryCode().trim())
                .tranSource(request.getSource().trim())
                .tranDesc(request.getDescription().trim())
                .tranAmt(request.getAmount())
                .tranCardNum(resolvedCardNum)
                .tranMerchantId(request.getMerchantId().trim())
                .tranMerchantName(request.getMerchantName().trim())
                .tranMerchantCity(request.getMerchantCity().trim())
                .tranMerchantZip(request.getMerchantZip().trim())
                .tranOrigTs(request.getOrigDate().trim())
                .tranProcTs(request.getProcDate().trim())
                .build();

        // EXEC CICS WRITE DATASET('TRANSACT') → repository.save()
        TransactionData saved = transactionRepository.save(record);
        log.info("COTRNADD: transaction saved, TRAN-ID={}", saved.getTranId());

        // Mirrors COBOL success message: "Transaction added successfully. Your Tran ID is <ID>."
        String message = String.format(
                "Transaction added successfully. Your Tran ID is %s.", saved.getTranId());

        return TransactionAddResponse.builder()
                .transactionId(saved.getTranId())
                .cardNumber(saved.getTranCardNum())
                .accountId(resolvedAcctId)
                .typeCode(saved.getTranTypeCd())
                .categoryCode(saved.getTranCatCd())
                .source(saved.getTranSource())
                .description(saved.getTranDesc())
                .amount(saved.getTranAmt())
                .origDate(saved.getTranOrigTs())
                .procDate(saved.getTranProcTs())
                .merchantId(saved.getTranMerchantId())
                .merchantName(saved.getTranMerchantName())
                .merchantCity(saved.getTranMerchantCity())
                .merchantZip(saved.getTranMerchantZip())
                .message(message)
                .build();
    }

    // ================================================================== private helpers

    /**
     * VALIDATE-INPUT-KEY-FIELDS paragraph.
     *
     * <p>The caller supplies either accountId OR cardNumber.  This method:
     * <ul>
     *   <li>If accountId provided → look up CXACAIX (alt-index on acct) to get card number</li>
     *   <li>If cardNumber provided → look up CCXREF (primary key) to get account ID</li>
     *   <li>Both missing → validation error matching COBOL message</li>
     * </ul>
     *
     * @return {@code [resolvedCardNum, resolvedAcctId]}
     */
    String[] resolveAndValidateKeys(TransactionAddRequest request) {

        boolean hasAcctId  = isPresent(request.getAccountId());
        boolean hasCardNum = isPresent(request.getCardNumber());

        if (!hasAcctId && !hasCardNum) {
            throw new TransactionValidationException(
                    "Account or Card Number must be entered...");
        }

        String resolvedCardNum;
        String resolvedAcctId;

        if (hasAcctId) {
            // COBOL: PERFORM READ-CXACAIX-FILE  (alternate-index lookup by acct)
            String acctId = request.getAccountId().trim();
            if (!acctId.matches("\\d+")) {
                throw new TransactionValidationException(
                        "Account ID must be Numeric...");
            }
            CardXref xref = cardXrefRepository.findByXrefAcctId(
                    String.format("%011d", Long.parseLong(acctId)))
                    .orElseThrow(() -> new TransactionValidationException(
                            "Account ID NOT found..."));
            resolvedCardNum = xref.getXrefCardNum();
            resolvedAcctId  = acctId;

        } else {
            // COBOL: PERFORM READ-CCXREF-FILE  (primary key lookup by card)
            String cardNum = request.getCardNumber().trim();
            if (!cardNum.matches("\\d+")) {
                throw new TransactionValidationException(
                        "Card Number must be Numeric...");
            }
            CardXref xref = cardXrefRepository.findByXrefCardNum(
                    String.format("%016d", Long.parseLong(cardNum)))
                    .orElseThrow(() -> new TransactionValidationException(
                            "Card Number NOT found..."));
            resolvedCardNum = cardNum;
            resolvedAcctId  = xref.getXrefAcctId();
        }

        return new String[]{resolvedCardNum, resolvedAcctId};
    }

    /**
     * VALIDATE-INPUT-DATA-FIELDS paragraph.
     *
     * <p>Validates every mandatory screen field exactly as the COBOL EVALUATE TRUE block does.
     */
    void validateDataFields(TransactionAddRequest request) {

        requireNonBlank(request.getTypeCode(),     "Type CD can NOT be empty...");
        requireNonBlank(request.getCategoryCode(), "Category CD can NOT be empty...");
        requireNonBlank(request.getSource(),       "Source can NOT be empty...");
        requireNonBlank(request.getDescription(),  "Description can NOT be empty...");
        requireNonNull (request.getAmount(),       "Amount can NOT be empty...");
        requireNonBlank(request.getOrigDate(),     "Orig Date can NOT be empty...");
        requireNonBlank(request.getProcDate(),     "Proc Date can NOT be empty...");
        requireNonBlank(request.getMerchantId(),   "Merchant ID can NOT be empty...");
        requireNonBlank(request.getMerchantName(), "Merchant Name can NOT be empty...");
        requireNonBlank(request.getMerchantCity(), "Merchant City can NOT be empty...");
        requireNonBlank(request.getMerchantZip(),  "Merchant Zip can NOT be empty...");

        // COBOL: TTYPCDI NOT NUMERIC
        if (!request.getTypeCode().trim().matches("\\d{1,2}")) {
            throw new TransactionValidationException("Type CD must be Numeric...");
        }

        // COBOL: TCATCDI NOT NUMERIC
        if (!request.getCategoryCode().trim().matches("\\d{1,4}")) {
            throw new TransactionValidationException("Category CD must be Numeric...");
        }

        // COBOL: Amount must be non-zero (amount > 0 requirement)
        if (request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new TransactionValidationException(
                    "Amount should be in format -99999999.99");
        }

        // COBOL: MIDI IS NOT NUMERIC
        if (!request.getMerchantId().trim().matches("\\d{1,9}")) {
            throw new TransactionValidationException("Merchant ID must be Numeric...");
        }

        // COBOL: TORIGDTI format YYYY-MM-DD
        validateDateFormat(request.getOrigDate(), "Orig Date should be in format YYYY-MM-DD");

        // COBOL: TPROCDTI format YYYY-MM-DD
        validateDateFormat(request.getProcDate(), "Proc Date should be in format YYYY-MM-DD");
    }

    /**
     * Generates the next 16-digit transaction ID.
     *
     * <p>Faithfully replicates the COBOL ADD-TRANSACTION ID generation logic:
     * <pre>
     *   MOVE HIGH-VALUES TO TRAN-ID
     *   EXEC CICS STARTBR … RIDFLD(TRAN-ID) …
     *   EXEC CICS READPREV … INTO(TRAN-RECORD) …
     *   EXEC CICS ENDBR …
     *   MOVE TRAN-ID TO WS-TRAN-ID-N
     *   ADD 1 TO WS-TRAN-ID-N
     * </pre>
     * HIGH-VALUES → findTopByOrderByTranIdDesc() (last record by descending key)
     * READPREV    → the first result of the above query
     * ADD 1       → increment + left-pad to 16 digits
     *
     * @return 16-character zero-padded transaction ID string
     */
    String generateNextTransactionId() {
        Optional<TransactionData> last = transactionRepository.findTopByOrderByTranIdDesc();
        long nextId = last.map(t -> {
            try {
                return Long.parseLong(t.getTranId().trim()) + 1L;
            } catch (NumberFormatException e) {
                log.warn("Non-numeric TRAN-ID '{}', starting from 1", t.getTranId());
                return 1L;
            }
        }).orElse(1L);

        return String.format("%0" + TRAN_ID_LENGTH + "d", nextId);
    }

    // ------------------------------------------------------------------ utilities

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    private static void requireNonBlank(String value, String message) {
        if (!isPresent(value)) {
            throw new TransactionValidationException(message);
        }
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new TransactionValidationException(message);
        }
    }

    private static void validateDateFormat(String date, String message) {
        if (!isPresent(date)) {
            throw new TransactionValidationException(message);
        }
        try {
            LocalDate.parse(date.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new TransactionValidationException(message);
        }
    }

    // ================================================================== inner exception

    /**
     * Mirrors COBOL validation error handling: each validation paragraph sets
     * WS-ERR-FLG = 'Y' and moves a message to WS-MESSAGE before PERFORM
     * SEND-TRNADD-SCREEN.  In REST terms we surface this as a 400 Bad Request.
     */
    public static class TransactionValidationException extends RuntimeException {
        public TransactionValidationException(String message) {
            super(message);
        }
    }
}
