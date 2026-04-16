package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COPAU01.cpy
 * COPAU1AI / COPAU1AO - BMS screen map for Pending Authorization detail screen.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingAuthDetailMap {

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

    /** CARDNUMI PIC X(16) - card number */
    private String cardnum;

    /** AUTHDTI PIC X(10) - authorization date */
    private String authdt;

    /** AUTHTMI PIC X(10) - authorization time */
    private String authtm;

    /** AUTHRSPI PIC X(1) - authorization response */
    private String authrsp;

    /** AUTHRSNI PIC X(20) - authorization response reason */
    private String authrsn;

    /** AUTHCDI PIC X(6) - authorization code */
    private String authcd;

    /** AUTHAMTI PIC X(12) - authorization amount */
    private String authamt;

    /** POSEMDI PIC X(4) - POS entry mode */
    private String posemd;

    /** AUTHSRCI PIC X(10) - authorization source */
    private String authsrc;

    /** MCCCDI PIC X(4) - merchant category code */
    private String mcccds;

    /** CRDEXPI PIC X(5) - card expiry */
    private String crdexp;

    /** AUTHTYPI PIC X(14) - authorization type */
    private String authtyp;

    /** TRNIDI PIC X(15) - transaction ID */
    private String trnid;

    /** AUTHMTCI PIC X(1) - match status */
    private String authmtc;

    /** AUTHFRDI PIC X(10) - fraud date */
    private String authfrd;

    /** MERNAMEI PIC X(25) - merchant name */
    private String mername;

    /** MERIDI PIC X(15) - merchant ID */
    private String merid;

    /** MERCITYI PIC X(25) - merchant city */
    private String mercity;

    /** MERSTI PIC X(2) - merchant state */
    private String merst;

    /** MERZIPI PIC X(10) - merchant zip */
    private String merzip;

    /** ERRMSGI PIC X(78) - error message */
    private String errmsg;
}
