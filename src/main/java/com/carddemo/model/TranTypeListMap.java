package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * Java model for COBOL copybook COTRTLI.cpy (CTRTLIAI / CTRTLIAO)
 * BMS screen map for Transaction Type List screen.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranTypeListMap {

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

    /** PAGENOI PIC X(3) - page number */
    private String pageno;

    /** TRTYPEI PIC X(2) - transaction type search filter */
    private String trtype;

    /** TRDESCI PIC X(50) - transaction type description filter */
    private String trdesc;

    /** INFOMSGI PIC X(45) - info message */
    private String infomsg;

    /** ERRMSGI PIC X(78) - error message */
    private String errmsg;

    /**
     * Inner class representing one row in the transaction type list.
     * Corresponds to TRTSEL/TRTTYP/TRTYPD occurring 1..8 times.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranTypeRow {
        /** TRTSELnI PIC X(1) - selection flag */
        private String trtsel;
        /** TRTTYPnI PIC X(2) - transaction type */
        private String trttyp;
        /** TRTYPDnI PIC X(50) - type description */
        private String trtypd;
    }

    /** Rows 1-7 + A (8 rows) of transaction types displayed */
    private List<TranTypeRow> rows = new ArrayList<>();
}
