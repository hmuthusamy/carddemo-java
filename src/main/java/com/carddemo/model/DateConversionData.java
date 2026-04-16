package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CODATECN.cpy
 * CODATECN-REC - Date conversion record structure.
 * Used as input/output for date format conversion utility (CODATECN).
 * Not a VSAM record - utility I/O structure, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateConversionData {

    // CODATECN-IN-REC group

    /** CODATECN-TYPE PIC X - input format: '1'=YYYYMMDD, '2'=YYYY-MM-DD */
    private String codatecnType;

    /** CODATECN-INP-DATE PIC X(20) - raw input date string */
    private String codatecnInpDate;

    // CODATECN-1INP (REDEFINES) sub-fields

    /** CODATECN-1YYYY PIC XXXX - year from format 1 */
    private String codatecn1Yyyy;

    /** CODATECN-1MM PIC XX - month from format 1 */
    private String codatecn1Mm;

    /** CODATECN-1DD PIC XX - day from format 1 */
    private String codatecn1Dd;

    // CODATECN-2INP (REDEFINES) sub-fields

    /** CODATECN-1O-YYYY PIC XXXX - year from format 2 */
    private String codatecn2Yyyy;

    /** CODATECN-1I-S1 PIC X - separator 1 */
    private String codatecn2Sep1;

    /** CODATECN-1MM PIC XX - month from format 2 */
    private String codatecn2Mm;

    /** CODATECN-1I-S2 PIC X - separator 2 */
    private String codatecn2Sep2;

    /** CODATECN-2YY PIC XX - 2-digit year from format 2 */
    private String codatecn2Yy;

    // CODATECN-OUT-REC group

    /** CODATECN-OUTTYPE PIC X - output format: '1'=YYYY-MM-DD, '2'=YYYYMMDD */
    private String codatecnOuttype;

    /** CODATECN-0UT-DATE PIC X(20) - raw output date string */
    private String codatecnOutDate;

    // CODATECN-1OUT (REDEFINES) sub-fields

    /** CODATECN-1O-YYYY PIC XXXX */
    private String codatecn1oYyyy;

    /** CODATECN-1O-S1 PIC X */
    private String codatecn1oSep1;

    /** CODATECN-1O-MM PIC XX */
    private String codatecn1oMm;

    /** CODATECN-1O-S2 PIC X */
    private String codatecn1oSep2;

    /** CODATECN-1O-DD PIC XX */
    private String codatecn1oDd;

    // CODATECN-2OUT (REDEFINES) sub-fields

    /** CODATECN-2O-YYYY PIC XXXX */
    private String codatecn2oYyyy;

    /** CODATECN-2O-MM PIC XX */
    private String codatecn2oMm;

    /** CODATECN-2O-DD PIC XX */
    private String codatecn2oDd;

    /** CODATECN-ERROR-MSG PIC X(38) */
    private String codatecnErrorMsg;
}
