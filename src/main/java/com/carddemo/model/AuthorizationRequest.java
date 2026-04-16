package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CCPAURQY.cpy
 * PA-RQ (Pending Authorization Request) - Authorization request fields.
 * Not a standalone VSAM entity - message request structure, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {

    /** PA-RQ-AUTH-DATE PIC X(06) */
    private String paRqAuthDate;

    /** PA-RQ-AUTH-TIME PIC X(06) */
    private String paRqAuthTime;

    /** PA-RQ-CARD-NUM PIC X(16) */
    private String paRqCardNum;

    /** PA-RQ-AUTH-TYPE PIC X(04) */
    private String paRqAuthType;

    /** PA-RQ-CARD-EXPIRY-DATE PIC X(04) */
    private String paRqCardExpiryDate;

    /** PA-RQ-MESSAGE-TYPE PIC X(06) */
    private String paRqMessageType;

    /** PA-RQ-MESSAGE-SOURCE PIC X(06) */
    private String paRqMessageSource;

    /** PA-RQ-PROCESSING-CODE PIC 9(06) */
    private Integer paRqProcessingCode;

    /** PA-RQ-TRANSACTION-AMT PIC +9(10).99 - display numeric amount */
    private java.math.BigDecimal paRqTransactionAmt;

    /** PA-RQ-MERCHANT-CATAGORY-CODE PIC X(04) */
    private String paRqMerchantCategoryCode;

    /** PA-RQ-ACQR-COUNTRY-CODE PIC X(03) */
    private String paRqAcqrCountryCode;

    /** PA-RQ-POS-ENTRY-MODE PIC 9(02) */
    private Integer paRqPosEntryMode;

    /** PA-RQ-MERCHANT-ID PIC X(15) */
    private String paRqMerchantId;

    /** PA-RQ-MERCHANT-NAME PIC X(22) */
    private String paRqMerchantName;

    /** PA-RQ-MERCHANT-CITY PIC X(13) */
    private String paRqMerchantCity;

    /** PA-RQ-MERCHANT-STATE PIC X(02) */
    private String paRqMerchantState;

    /** PA-RQ-MERCHANT-ZIP PIC X(09) */
    private String paRqMerchantZip;

    /** PA-RQ-TRANSACTION-ID PIC X(15) */
    private String paRqTransactionId;
}
