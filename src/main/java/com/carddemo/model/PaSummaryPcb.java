package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook PASFLPCB.CPY
 * PASFLPCB - IMS Program Communication Block (PCB) for PA summary flat file.
 * Not a VSAM record - IMS PCB control block, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaSummaryPcb {

    /** PASFL-DBDNAME PIC X(08) */
    private String pasflDbdname;

    /** PASFL-SEG-LEVEL PIC X(02) */
    private String pasflSegLevel;

    /** PASFL-PCB-STATUS PIC X(02) */
    private String pasflPcbStatus;

    /** PASFL-PCB-PROCOPT PIC X(04) */
    private String pasflPcbProcopt;

    /** PASFL-SEG-NAME PIC X(08) */
    private String pasflSegName;

    /** PASFL-KEYFB-NAME PIC S9(05) COMP */
    private Integer pasflKeyfbName;

    /** PASFL-NUM-SENSEGS PIC S9(05) COMP */
    private Integer pasflNumSensegs;

    /** PASFL-KEYFB PIC X(100) */
    private String pasflKeyfb;
}
