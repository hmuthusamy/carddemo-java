package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSDAT01Y.cpy
 * WS-DATE-TIME - Working storage date/time structure.
 * Not a VSAM record - working storage area, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateData {

    // WS-CURDATE-DATA group

    /** WS-CURDATE-YEAR PIC 9(04) */
    private Integer wsCurdateYear;

    /** WS-CURDATE-MONTH PIC 9(02) */
    private Integer wsCurdateMonth;

    /** WS-CURDATE-DAY PIC 9(02) */
    private Integer wsCurdateDay;

    /** WS-CURTIME-HOURS PIC 9(02) */
    private Integer wsCurtimeHours;

    /** WS-CURTIME-MINUTE PIC 9(02) */
    private Integer wsCurtimeMinute;

    /** WS-CURTIME-SECOND PIC 9(02) */
    private Integer wsCurtimeSecond;

    /** WS-CURTIME-MILSEC PIC 9(02) */
    private Integer wsCurtimeMilsec;

    // WS-CURDATE-MM-DD-YY group

    /** WS-CURDATE-MM PIC 9(02) */
    private Integer wsCurdateMm;

    /** WS-CURDATE-DD PIC 9(02) */
    private Integer wsCurdateDd;

    /** WS-CURDATE-YY PIC 9(02) */
    private Integer wsCurdateYy;

    // WS-CURTIME-HH-MM-SS group

    /** WS-CURTIME-HH PIC 9(02) */
    private Integer wsCurtimeHh;

    /** WS-CURTIME-MM PIC 9(02) */
    private Integer wsCurtimeMm;

    /** WS-CURTIME-SS PIC 9(02) */
    private Integer wsCurtimeSs;

    // WS-TIMESTAMP group

    /** WS-TIMESTAMP-DT-YYYY PIC 9(04) */
    private Integer wsTimestampDtYyyy;

    /** WS-TIMESTAMP-DT-MM PIC 9(02) */
    private Integer wsTimestampDtMm;

    /** WS-TIMESTAMP-DT-DD PIC 9(02) */
    private Integer wsTimestampDtDd;

    /** WS-TIMESTAMP-TM-HH PIC 9(02) */
    private Integer wsTimestampTmHh;

    /** WS-TIMESTAMP-TM-MM PIC 9(02) */
    private Integer wsTimestampTmMm;

    /** WS-TIMESTAMP-TM-SS PIC 9(02) */
    private Integer wsTimestampTmSs;

    /** WS-TIMESTAMP-TM-MS6 PIC 9(06) */
    private Integer wsTimestampTmMs6;
}
