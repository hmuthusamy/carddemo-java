package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook UNUSED1Y.cpy
 * UNUSED-DATA - Unused security user data record (placeholder).
 * Not a VSAM record - unused/deprecated structure, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnusedData {

    /** UNUSED-ID PIC X(08) */
    private String unusedId;

    /** UNUSED-FNAME PIC X(20) */
    private String unusedFname;

    /** UNUSED-LNAME PIC X(20) */
    private String unusedLname;

    /** UNUSED-PWD PIC X(08) */
    private String unusedPwd;

    /** UNUSED-TYPE PIC X(01) */
    private String unusedType;

    /** UNUSED-FILLER PIC X(23) */
    private String unusedFiller;
}
