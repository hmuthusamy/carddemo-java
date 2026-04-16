package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook PADFLPCB.CPY
 * PADFLPCB - IMS Program Communication Block (PCB) for PA detail flat file.
 * Not a VSAM record - IMS PCB control block, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaDetailPcb {

    /** PADFL-DBDNAME PIC X(08) */
    private String padflDbdname;

    /** PADFL-SEG-LEVEL PIC X(02) */
    private String padflSegLevel;

    /** PADFL-PCB-STATUS PIC X(02) */
    private String padflPcbStatus;

    /** PADFL-PCB-PROCOPT PIC X(04) */
    private String padflPcbProcopt;

    /** PADFL-SEG-NAME PIC X(08) */
    private String padflSegName;

    /** PADFL-KEYFB-NAME PIC S9(05) COMP */
    private Integer padflKeyfbName;

    /** PADFL-NUM-SENSEGS PIC S9(05) COMP */
    private Integer padflNumSensegs;

    /** PADFL-KEYFB PIC X(255) */
    private String padflKeyfb;
}
