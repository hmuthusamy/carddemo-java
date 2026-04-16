package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVTRA07Y.cpy
 * Transaction report structures (REPORT-NAME-HEADER, TRANSACTION-DETAIL-REPORT, etc.)
 * Not a VSAM entity - reporting/display structures, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReport {

    // REPORT-NAME-HEADER fields

    /** REPT-SHORT-NAME PIC X(38) */
    private String reptShortName;

    /** REPT-LONG-NAME PIC X(41) */
    private String reptLongName;

    /** REPT-DATE-HEADER PIC X(12) */
    private String reptDateHeader;

    /** REPT-START-DATE PIC X(10) */
    private String reptStartDate;

    /** REPT-END-DATE PIC X(10) */
    private String reptEndDate;

    // TRANSACTION-DETAIL-REPORT fields

    /** TRAN-REPORT-TRANS-ID PIC X(16) */
    private String tranReportTransId;

    /** TRAN-REPORT-ACCOUNT-ID PIC X(11) */
    private String tranReportAccountId;

    /** TRAN-REPORT-TYPE-CD PIC X(02) */
    private String tranReportTypeCd;

    /** TRAN-REPORT-TYPE-DESC PIC X(15) */
    private String tranReportTypeDesc;

    /** TRAN-REPORT-CAT-CD PIC 9(04) */
    private Integer tranReportCatCd;

    /** TRAN-REPORT-CAT-DESC PIC X(29) */
    private String tranReportCatDesc;

    /** TRAN-REPORT-SOURCE PIC X(10) */
    private String tranReportSource;

    /** TRAN-REPORT-AMT - formatted display numeric */
    private BigDecimal tranReportAmt;

    // REPORT-PAGE-TOTALS

    /** REPT-PAGE-TOTAL - formatted display numeric */
    private BigDecimal reptPageTotal;

    // REPORT-ACCOUNT-TOTALS

    /** REPT-ACCOUNT-TOTAL - formatted display numeric */
    private BigDecimal reptAccountTotal;

    // REPORT-GRAND-TOTALS

    /** REPT-GRAND-TOTAL - formatted display numeric */
    private BigDecimal reptGrandTotal;
}
