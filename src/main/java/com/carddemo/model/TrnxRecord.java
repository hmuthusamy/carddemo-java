package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COSTM01.CPY
 * TRNX-RECORD - Transaction record with key structure for VSAM.
 * Used for reporting/export; represented as a model class.
 * Not a standalone entity (subset of transaction data).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrnxRecord {

    // TRNX-KEY group

    /** TRNX-CARD-NUM PIC X(16) */
    private String trnxCardNum;

    /** TRNX-ID PIC X(16) */
    private String trnxId;

    // TRNX-REST group

    /** TRNX-TYPE-CD PIC X(02) */
    private String trnxTypeCd;

    /** TRNX-CAT-CD PIC 9(04) */
    private Integer trnxCatCd;

    /** TRNX-SOURCE PIC X(10) */
    private String trnxSource;

    /** TRNX-DESC PIC X(100) */
    private String trnxDesc;

    /** TRNX-AMT PIC S9(09)V99 - signed decimal */
    private java.math.BigDecimal trnxAmt;

    /** TRNX-MERCHANT-ID PIC 9(09) */
    private Long trnxMerchantId;

    /** TRNX-MERCHANT-NAME PIC X(50) */
    private String trnxMerchantName;

    /** TRNX-MERCHANT-CITY PIC X(50) */
    private String trnxMerchantCity;

    /** TRNX-MERCHANT-ZIP PIC X(10) */
    private String trnxMerchantZip;

    /** TRNX-ORIG-TS PIC X(26) */
    private String trnxOrigTs;

    /** TRNX-PROC-TS PIC X(26) */
    private String trnxProcTs;
}
