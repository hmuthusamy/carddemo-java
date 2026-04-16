package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSDB2RPY.cpy
 * Db2 Common Procedure copybook (working storage portion from CSDB2RWY).
 * Not a VSAM record - Db2 working storage variables, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Db2CommonData {

    /** WS-DISP-SQLCODE PIC ----9 - display SQL return code */
    private Integer wsDispSqlcode;

    /** WS-DUMMY-DB2-INT PIC S9(4) COMP-3 */
    private java.math.BigDecimal wsDummyDb2Int;

    /** WS-DB2-PROCESSING-FLAG PIC X(1) - '0'=OK, '1'=Error */
    private String wsDb2ProcessingFlag;

    /** WS-DB2-CURRENT-ACTION PIC X(72) */
    private String wsDb2CurrentAction;

    /** WS-DSNTIAC-MESG-LEN PIC S9(4) COMP */
    private Integer wsDsntiacMesgLen;

    /** WS-DSNTIAC-FMTD-TEXT - 10 lines of 72 chars */
    private String[] wsDsntiacFmtdTextLines = new String[10];

    /** WS-DSNTIAC-LRECL PIC S9(4) COMP */
    private Integer wsDsntiacLrecl;

    /** WS-DSNTIAC-ERR-MSG PIC X(10) */
    private String wsDsntiacErrMsg;

    /** WS-DSNTIAC-ERR-CD PIC 9(02) */
    private Integer wsDsntiacErrCd;
}
