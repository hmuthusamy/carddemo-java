package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook PAUTBPCB.CPY
 * PAUTBPCB - IMS Program Communication Block (PCB) for PA utility/transaction block.
 * Not a VSAM record - IMS PCB control block, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaUtilityPcb {

    /** PAUT-DBDNAME PIC X(08) */
    private String pautDbdname;

    /** PAUT-SEG-LEVEL PIC X(02) */
    private String pautSegLevel;

    /** PAUT-PCB-STATUS PIC X(02) */
    private String pautPcbStatus;

    /** PAUT-PCB-PROCOPT PIC X(04) */
    private String pautPcbProcopt;

    /** PAUT-SEG-NAME PIC X(08) */
    private String pautSegName;

    /** PAUT-KEYFB-NAME PIC S9(05) COMP */
    private Integer pautKeyfbName;

    /** PAUT-NUM-SENSEGS PIC S9(05) COMP */
    private Integer pautNumSensegs;

    /** PAUT-KEYFB PIC X(255) */
    private String pautKeyfb;
}
