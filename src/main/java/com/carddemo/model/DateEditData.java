package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSUTLDWY.cpy
 * Working Storage Copybook for DATE related code.
 * Not a VSAM record - date validation working storage, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateEditData {

    /** WS-EDIT-DATE-CC PIC X(2) - century portion */
    private String wsEditDateCc;

    /** WS-EDIT-DATE-YY PIC X(2) - year within century */
    private String wsEditDateYy;

    /** WS-EDIT-DATE-MM PIC X(2) - month */
    private String wsEditDateMm;

    /** WS-EDIT-DATE-DD PIC X(2) - day */
    private String wsEditDateDd;

    /** WS-EDIT-DATE-BINARY PIC S9(9) COMP - binary date for computation */
    private Integer wsEditDateBinary;

    /** WS-CURRENT-DATE-YYYYMMDD PIC X(8) - current date */
    private String wsCurrentDateYyyymmdd;

    /** WS-CURRENT-DATE-BINARY PIC S9(9) COMP - binary current date */
    private Integer wsCurrentDateBinary;

    // WS-EDIT-DATE-FLGS group

    /** WS-EDIT-YEAR-FLG PIC X(01) - year validation flag */
    private String wsEditYearFlg;

    /** WS-EDIT-MONTH PIC X(01) - month validation flag */
    private String wsEditMonth;

    /** WS-EDIT-DAY PIC X(01) - day validation flag */
    private String wsEditDay;

    /** WS-DATE-FORMAT PIC X(08) */
    private String wsDateFormat;

    // WS-DATE-VALIDATION-RESULT group

    /** WS-SEVERITY PIC X(04) */
    private String wsSeverity;

    /** WS-MSG-NO PIC X(04) */
    private String wsMsgNo;

    /** WS-RESULT PIC X(15) */
    private String wsResult;

    /** WS-DATE PIC X(10) */
    private String wsDate;

    /** WS-DATE-FMT PIC X(10) */
    private String wsDateFmt;
}
