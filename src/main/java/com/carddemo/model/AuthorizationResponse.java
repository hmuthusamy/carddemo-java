package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CCPAURLY.cpy
 * PA-RL (Pending Authorization Response Layout) - Authorization response fields.
 * Not a standalone VSAM entity - message response structure, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    /** PA-RL-CARD-NUM PIC X(16) */
    private String paRlCardNum;

    /** PA-RL-TRANSACTION-ID PIC X(15) */
    private String paRlTransactionId;

    /** PA-RL-AUTH-ID-CODE PIC X(06) */
    private String paRlAuthIdCode;

    /** PA-RL-AUTH-RESP-CODE PIC X(02) */
    private String paRlAuthRespCode;

    /** PA-RL-AUTH-RESP-REASON PIC X(04) */
    private String paRlAuthRespReason;

    /** PA-RL-APPROVED-AMT PIC +9(10).99 - display numeric amount */
    private java.math.BigDecimal paRlApprovedAmt;
}
