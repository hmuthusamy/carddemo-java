package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook IMSFUNCS.cpy
 * FUNC-CODES - IMS function code constants (GU, GHU, GN, etc.).
 * Not a VSAM record - IMS control constants, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImsFunctionCodes {

    /** FUNC-GU PIC X(04) VALUE 'GU  ' */
    private String funcGu = "GU  ";

    /** FUNC-GHU PIC X(04) VALUE 'GHU ' */
    private String funcGhu = "GHU ";

    /** FUNC-GN PIC X(04) VALUE 'GN  ' */
    private String funcGn = "GN  ";

    /** FUNC-GHN PIC X(04) VALUE 'GHN ' */
    private String funcGhn = "GHN ";

    /** FUNC-GNP PIC X(04) VALUE 'GNP ' */
    private String funcGnp = "GNP ";

    /** FUNC-GHNP PIC X(04) VALUE 'GHNP' */
    private String funcGhnp = "GHNP";

    /** FUNC-REPL PIC X(04) VALUE 'REPL' */
    private String funcRepl = "REPL";

    /** FUNC-ISRT PIC X(04) VALUE 'ISRT' */
    private String funcIsrt = "ISRT";

    /** FUNC-DLET PIC X(04) VALUE 'DLET' */
    private String funcDlet = "DLET";

    /** PARMCOUNT PIC S9(05) VALUE +4 COMP-5 */
    private Integer parmcount = 4;
}
