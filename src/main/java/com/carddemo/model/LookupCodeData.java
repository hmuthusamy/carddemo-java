package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSLKPCDY.cpy
 * WS-US-PHONE-AREA-CODE-TO-EDIT / US-STATE-CODE-TO-EDIT / US-STATE-ZIPCODE-TO-EDIT
 * Validation lookup tables for phone area codes, state codes, and state+zip combos.
 * Not a VSAM record - validation reference data, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LookupCodeData {

    /** WS-US-PHONE-AREA-CODE-TO-EDIT PIC XXX - the area code being validated */
    private String wsUsPhoneAreaCodeToEdit;

    /** US-STATE-CODE-TO-EDIT PIC X(2) - the state code being validated */
    private String usStateCodeToEdit;

    /** US-STATE-AND-FIRST-ZIP2 PIC X(4) - state + first 2 digits of zip */
    private String usStateAndFirstZip2;

    /** LAST-3-OF-ZIP PIC X(3) - last 3 digits of zip code */
    private String last3OfZip;
}
