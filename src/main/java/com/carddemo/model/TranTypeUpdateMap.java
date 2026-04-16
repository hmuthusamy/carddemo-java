package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COTRTUP.cpy (CTRTUPAI / CTRTUPAO)
 * BMS screen map for Transaction Type Update screen.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranTypeUpdateMap {

    /** TRNNAMEI PIC X(4) */
    private String trnname;

    /** TITLE01I PIC X(40) */
    private String title01;

    /** CURDATEI PIC X(8) */
    private String curdate;

    /** PGMNAMEI PIC X(8) */
    private String pgmname;

    /** TITLE02I PIC X(40) */
    private String title02;

    /** CURTIMEI PIC X(8) */
    private String curtime;

    /** TRTYPCDI PIC X(2) - transaction type code */
    private String trtypcd;

    /** TRTYDSCI PIC X(50) - transaction type description */
    private String trtydsc;

    /** INFOMSGI PIC X(45) - info message */
    private String infomsg;

    /** ERRMSGI PIC X(78) - error message */
    private String errmsg;

    /** FKEYSI PIC X(21) - function keys legend */
    private String fkeys;

    /** FKEY04I PIC X(9) - F4 label */
    private String fkey04;

    /** FKEY05I PIC X(8) - F5 label */
    private String fkey05;

    /** FKEY06I PIC X(6) - F6 label */
    private String fkey06;

    /** FKEY12I PIC X(10) - F12 label */
    private String fkey12;
}
