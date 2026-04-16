package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COPAU00.cpy
 * COPAU0AI / COPAU0AO - BMS screen map for Pending Authorization list screen.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 * Models the input (I) fields of the screen map.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingAuthListMap {

    /** TRNNAMEI PIC X(4) - transaction name */
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

    /** ACCTIDI PIC X(11) - account ID */
    private String acctid;

    /** CNAMEI PIC X(25) - customer name */
    private String cname;

    /** CUSTIDI PIC X(9) - customer ID */
    private String custid;

    /** ADDR001I PIC X(25) - address */
    private String addr001;

    /** ACCSTATI PIC X(1) - account status */
    private String accstat;

    /** ADDR002I PIC X(25) */
    private String addr002;

    /** PHONE1I PIC X(13) */
    private String phone1;

    /** APPRCNTI PIC X(3) - approved count */
    private String apprcnt;

    /** DECLCNTI PIC X(3) - declined count */
    private String declcnt;

    /** CREDLIMI PIC X(12) - credit limit */
    private String credlim;

    /** CASHLIMI PIC X(9) - cash limit */
    private String cashlim;

    /** APPRAMTI PIC X(10) - approved amount */
    private String appramt;

    /** CREDBALI PIC X(12) - credit balance */
    private String credbal;

    /** CASHBALI PIC X(9) - cash balance */
    private String cashbal;

    /** DECLAMTI PIC X(10) - declined amount */
    private String declamt;

    /** ERRMSGI PIC X(78) - error message */
    private String errmsg;
}
