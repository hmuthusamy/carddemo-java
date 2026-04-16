package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSMSG02Y.cpy
 * ABEND-DATA - Work areas for abend (abnormal end) routine.
 * Not a VSAM record - diagnostic work area, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbendData {

    /** ABEND-CODE PIC X(4) */
    private String abendCode;

    /** ABEND-CULPRIT PIC X(8) */
    private String abendCulprit;

    /** ABEND-REASON PIC X(50) */
    private String abendReason;

    /** ABEND-MSG PIC X(72) */
    private String abendMsg;
}
