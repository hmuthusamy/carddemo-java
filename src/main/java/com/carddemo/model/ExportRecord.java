package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVEXPORT.cpy
 * EXPORT-RECORD - Multi-record export layout for sequential export file.
 * Total Record Length: 500 bytes.
 * Not a standalone VSAM entity - sequential file export record, no @Entity.
 * Contains nested classes for each record type variant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRecord {

    /** EXPORT-REC-TYPE PIC X(1) - record type discriminator */
    private String exportRecType;

    /** EXPORT-TIMESTAMP PIC X(26) */
    private String exportTimestamp;

    /** EXPORT-SEQUENCE-NUM PIC 9(9) COMP */
    private Long exportSequenceNum;

    /** EXPORT-BRANCH-ID PIC X(4) */
    private String exportBranchId;

    /** EXPORT-REGION-CODE PIC X(5) */
    private String exportRegionCode;

    /** EXPORT-RECORD-DATA PIC X(460) - raw payload bytes as String */
    private String exportRecordData;

    // ---------------------------------------------------------------
    // Nested class for Customer record type (REDEFINES EXPORT-RECORD-DATA)
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportCustomerData {
        /** EXP-CUST-ID PIC 9(09) COMP */
        private Long expCustId;
        /** EXP-CUST-FIRST-NAME PIC X(25) */
        private String expCustFirstName;
        /** EXP-CUST-MIDDLE-NAME PIC X(25) */
        private String expCustMiddleName;
        /** EXP-CUST-LAST-NAME PIC X(25) */
        private String expCustLastName;
        /** EXP-CUST-ADDR-LINE OCCURS 3 TIMES PIC X(50) */
        private String[] expCustAddrLines = new String[3];
        /** EXP-CUST-ADDR-STATE-CD PIC X(02) */
        private String expCustAddrStateCd;
        /** EXP-CUST-ADDR-COUNTRY-CD PIC X(03) */
        private String expCustAddrCountryCd;
        /** EXP-CUST-ADDR-ZIP PIC X(10) */
        private String expCustAddrZip;
        /** EXP-CUST-PHONE-NUM OCCURS 2 TIMES PIC X(15) */
        private String[] expCustPhoneNums = new String[2];
        /** EXP-CUST-SSN PIC 9(09) */
        private Long expCustSsn;
        /** EXP-CUST-GOVT-ISSUED-ID PIC X(20) */
        private String expCustGovtIssuedId;
        /** EXP-CUST-DOB-YYYY-MM-DD PIC X(10) */
        private String expCustDobYyyyMmDd;
        /** EXP-CUST-EFT-ACCOUNT-ID PIC X(10) */
        private String expCustEftAccountId;
        /** EXP-CUST-PRI-CARD-HOLDER-IND PIC X(01) */
        private String expCustPriCardHolderInd;
        /** EXP-CUST-FICO-CREDIT-SCORE PIC 9(03) COMP-3 */
        private BigDecimal expCustFicoCreditScore;
    }

    // ---------------------------------------------------------------
    // Nested class for Account record type (REDEFINES EXPORT-RECORD-DATA)
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportAccountData {
        /** EXP-ACCT-ID PIC 9(11) */
        private Long expAcctId;
        /** EXP-ACCT-ACTIVE-STATUS PIC X(01) */
        private String expAcctActiveStatus;
        /** EXP-ACCT-CURR-BAL PIC S9(10)V99 COMP-3 */
        private BigDecimal expAcctCurrBal;
        /** EXP-ACCT-CREDIT-LIMIT PIC S9(10)V99 */
        private BigDecimal expAcctCreditLimit;
        /** EXP-ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3 */
        private BigDecimal expAcctCashCreditLimit;
        /** EXP-ACCT-OPEN-DATE PIC X(10) */
        private String expAcctOpenDate;
        /** EXP-ACCT-EXPIRAION-DATE PIC X(10) */
        private String expAcctExpirationDate;
        /** EXP-ACCT-REISSUE-DATE PIC X(10) */
        private String expAcctReissueDate;
        /** EXP-ACCT-CURR-CYC-CREDIT PIC S9(10)V99 */
        private BigDecimal expAcctCurrCycCredit;
        /** EXP-ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP */
        private BigDecimal expAcctCurrCycDebit;
        /** EXP-ACCT-ADDR-ZIP PIC X(10) */
        private String expAcctAddrZip;
        /** EXP-ACCT-GROUP-ID PIC X(10) */
        private String expAcctGroupId;
    }

    // ---------------------------------------------------------------
    // Nested class for Transaction record type (REDEFINES EXPORT-RECORD-DATA)
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportTransactionData {
        /** EXP-TRAN-ID PIC X(16) */
        private String expTranId;
        /** EXP-TRAN-TYPE-CD PIC X(02) */
        private String expTranTypeCd;
        /** EXP-TRAN-CAT-CD PIC 9(04) */
        private Integer expTranCatCd;
        /** EXP-TRAN-SOURCE PIC X(10) */
        private String expTranSource;
        /** EXP-TRAN-DESC PIC X(100) */
        private String expTranDesc;
        /** EXP-TRAN-AMT PIC S9(09)V99 COMP-3 */
        private BigDecimal expTranAmt;
        /** EXP-TRAN-MERCHANT-ID PIC 9(09) COMP */
        private Long expTranMerchantId;
        /** EXP-TRAN-MERCHANT-NAME PIC X(50) */
        private String expTranMerchantName;
        /** EXP-TRAN-MERCHANT-CITY PIC X(50) */
        private String expTranMerchantCity;
        /** EXP-TRAN-MERCHANT-ZIP PIC X(10) */
        private String expTranMerchantZip;
        /** EXP-TRAN-CARD-NUM PIC X(16) */
        private String expTranCardNum;
        /** EXP-TRAN-ORIG-TS PIC X(26) */
        private String expTranOrigTs;
        /** EXP-TRAN-PROC-TS PIC X(26) */
        private String expTranProcTs;
    }

    // ---------------------------------------------------------------
    // Nested class for Card Cross-Reference record type
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportCardXrefData {
        /** EXP-XREF-CARD-NUM PIC X(16) */
        private String expXrefCardNum;
        /** EXP-XREF-CUST-ID PIC 9(09) */
        private Long expXrefCustId;
        /** EXP-XREF-ACCT-ID PIC 9(11) COMP */
        private Long expXrefAcctId;
    }

    // ---------------------------------------------------------------
    // Nested class for Card record type
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportCardData {
        /** EXP-CARD-NUM PIC X(16) */
        private String expCardNum;
        /** EXP-CARD-ACCT-ID PIC 9(11) COMP */
        private Long expCardAcctId;
        /** EXP-CARD-CVV-CD PIC 9(03) COMP */
        private Integer expCardCvvCd;
        /** EXP-CARD-EMBOSSED-NAME PIC X(50) */
        private String expCardEmbossedName;
        /** EXP-CARD-EXPIRAION-DATE PIC X(10) */
        private String expCardExpirationDate;
        /** EXP-CARD-ACTIVE-STATUS PIC X(01) */
        private String expCardActiveStatus;
    }
}
