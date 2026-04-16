package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TransactionAddResponse – REST DTO returned by POST /api/transactions.
 *
 * Mirrors the success message produced by COBOL WRITE-TRANSACT-FILE paragraph:
 *   "Transaction added successfully. Your Tran ID is &lt;TRAN-ID&gt;."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAddResponse {

    /** Generated transaction ID – mirrors COBOL TRAN-ID PIC X(16) */
    private String transactionId;

    /** Resolved card number after CCXREF/CXACAIX lookup */
    private String cardNumber;

    /** Resolved account ID after CCXREF/CXACAIX lookup */
    private String accountId;

    /** Transaction type code */
    private String typeCode;

    /** Category code */
    private String categoryCode;

    /** Source */
    private String source;

    /** Description */
    private String description;

    /** Amount */
    private BigDecimal amount;

    /** Origination date (YYYY-MM-DD) */
    private String origDate;

    /** Processing date (YYYY-MM-DD) */
    private String procDate;

    /** Merchant ID */
    private String merchantId;

    /** Merchant name */
    private String merchantName;

    /** Merchant city */
    private String merchantCity;

    /** Merchant zip */
    private String merchantZip;

    /** Human-readable status message matching COBOL success string */
    private String message;
}
