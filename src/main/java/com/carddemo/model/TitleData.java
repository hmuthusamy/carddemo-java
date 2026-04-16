package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COTTL01Y.cpy
 * CCDA-SCREEN-TITLE - Screen title constants.
 * Not a VSAM record - UI/display data, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TitleData {

    /** CCDA-TITLE01 PIC X(40) */
    private String ccdaTitle01;

    /** CCDA-TITLE02 PIC X(40) */
    private String ccdaTitle02;

    /** CCDA-THANK-YOU PIC X(40) */
    private String ccdaThankYou;
}
