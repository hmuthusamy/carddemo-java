package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TransactionAddRequest – REST DTO for POST /api/transactions.
 *
 * Maps directly to COTRN2AI screen fields from COBOL COTRN02C / COTRNADD:
 *
 *   ACTIDINI  → accountId     (WS-ACCT-ID-N   PIC 9(11))
 *   CARDNINI  → cardNumber    (WS-CARD-NUM-N   PIC 9(16))
 *   TTYPCDI   → typeCode      (TRAN-TYPE-CD    PIC X(02))
 *   TCATCDI   → categoryCode  (TRAN-CAT-CD     PIC 9(04))
 *   TRNSRCI   → source        (TRAN-SOURCE     PIC X(10))
 *   TDESCI    → description   (TRAN-DESC       PIC X(48))
 *   TRNAMTI   → amount        (TRAN-AMT        PIC S9(9)V99)
 *   TORIGDTI  → origDate      (TRAN-ORIG-TS    format YYYY-MM-DD)
 *   TPROCDTI  → procDate      (TRAN-PROC-TS    format YYYY-MM-DD)
 *   MIDI      → merchantId    (TRAN-MERCHANT-ID   PIC 9(09))
 *   MNAMEI    → merchantName  (TRAN-MERCHANT-NAME PIC X(50))
 *   MCITYI    → merchantCity  (TRAN-MERCHANT-CITY PIC X(50))
 *   MZIPI     → merchantZip   (TRAN-MERCHANT-ZIP  PIC X(10))
 *
 * Caller may supply either accountId OR cardNumber; the service resolves
 * the missing half via the CCXREF / CXACAIX lookup – mirroring the COBOL
 * VALIDATE-INPUT-KEY-FIELDS paragraph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAddRequest {

    // ------------------------------------------------------------------ key fields
    /** ACTIDINI – account ID (optional when cardNumber provided) */
    private String accountId;

    /** CARDNINI – 16-digit card number (optional when accountId provided) */
    private String cardNumber;

    // ------------------------------------------------------------------ data fields
    /** TTYPCDI – transaction type code (2-char numeric string, e.g. "01") */
    private String typeCode;

    /** TCATCDI – transaction category code (4-digit numeric string, e.g. "0001") */
    private String categoryCode;

    /** TRNSRCI – transaction source (max 10 chars) */
    private String source;

    /** TDESCI – human-readable description (max 48 chars) */
    private String description;

    /**
     * TRNAMTI – signed amount in format ±99999999.99.
     * Positive for credits, negative for debits (matches COBOL PIC S9(9)V99).
     */
    private BigDecimal amount;

    /** TORIGDTI – origination date (YYYY-MM-DD) */
    private String origDate;

    /** TPROCDTI – processing date (YYYY-MM-DD) */
    private String procDate;

    /** MIDI – merchant ID (9-digit numeric string) */
    private String merchantId;

    /** MNAMEI – merchant name (max 50 chars) */
    private String merchantName;

    /** MCITYI – merchant city (max 50 chars) */
    private String merchantCity;

    /** MZIPI – merchant zip code (max 10 chars) */
    private String merchantZip;
}
