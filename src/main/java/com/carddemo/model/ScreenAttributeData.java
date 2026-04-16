package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSSETATY.cpy
 * CSSETATY - Screen attribute setting procedure copybook.
 * This copybook contains inline COBOL logic (SET attribute to red).
 * Modelled as a simple attribute holder for screen field state.
 * Not a VSAM record - screen attribute data, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenAttributeData {

    /** Test variable name for screen attribute (TESTVAR1 placeholder) */
    private String testVarName;

    /** Screen variable name (SCRNVAR2 placeholder) */
    private String screenVarName;

    /** Map name (MAPNAME3 placeholder) */
    private String mapName;

    /** Flag indicating whether the field is in error */
    private boolean fieldInError;

    /** Flag indicating whether the field is blank */
    private boolean fieldBlank;
}
