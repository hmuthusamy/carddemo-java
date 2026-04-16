package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COACTUP.CPY
 * CACTUPAI - Account Update screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateMap {

    private String trnname;
    private String title01;
    private String curdate;
    private String pgmname;
    private String title02;
    private String curtime;

    /** ACCTSIDI PIC X(11) - account ID */
    private String acctsid;

    /** ACSTTUSI PIC X(1) - account status */
    private String acsttus;

    /** OPNYEARI PIC X(4) - open year */
    private String opnyear;

    /** OPNMONI PIC X(2) - open month */
    private String opnmon;

    /** OPNDAYI PIC X(2) - open day */
    private String opnday;

    /** ACRDLIMI PIC X(15) - credit limit */
    private String acrdlim;

    /** EXPYEARI PIC X(4) - expiry year */
    private String expyear;

    /** EXPMONI PIC X(2) - expiry month */
    private String expmon;

    /** EXPDAYI PIC X(2) - expiry day */
    private String expday;

    /** ACSHLIMI PIC X(15) - cash limit */
    private String acshlim;

    /** RISYEARI PIC X(4) - reissue year */
    private String risyear;

    /** RISMONI PIC X(2) - reissue month */
    private String rismon;

    /** RISDAYI PIC X(2) - reissue day */
    private String risday;

    /** ACURBALI PIC X(15) - current balance */
    private String acurbal;

    /** ACRCYCRI PIC X(15) - cycle credit */
    private String acrcycr;

    /** AADDGRPI PIC X(10) - group ID */
    private String aaddgrp;

    /** ACRCYDBI PIC X(15) - cycle debit */
    private String acrcydb;

    /** ACSTNUMI PIC X(9) - customer number */
    private String acstnum;

    /** ACTSSN1I PIC X(3) - SSN part 1 */
    private String actssn1;

    /** ACTSSN2I PIC X(2) - SSN part 2 */
    private String actssn2;

    /** ACTSSN3I PIC X(4) - SSN part 3 */
    private String actssn3;

    /** DOBYEARI PIC X(4) - DOB year */
    private String dobyear;

    /** DOBMONI PIC X(2) - DOB month */
    private String dobmon;

    /** DOBDAYI PIC X(2) - DOB day */
    private String dobday;

    /** ACSTFCOI PIC X(3) - FICO score */
    private String acstfco;

    /** ACSFNAMI PIC X(25) - first name */
    private String acsfnam;

    /** ACSMNIMI PIC X(25) - middle name */
    private String acsmnam;

    /** ACSLNAMI PIC X(25) - last name */
    private String acslnam;

    /** ACSADL1I PIC X(50) - address line 1 */
    private String acsadl1;

    /** ACSSTTEI PIC X(2) - state */
    private String acsstte;

    /** ACSADL2I PIC X(50) - address line 2 */
    private String acsadl2;

    /** ACSZIPCI PIC X(5) - zip code */
    private String acszipc;

    /** ACSCITYI PIC X(50) - city */
    private String acscity;

    /** ACSCTRYI PIC X(3) - country */
    private String acsctry;

    /** ACSPH1AI PIC X(3) - phone 1 area */
    private String acsph1a;

    /** ACSPH1BI PIC X(3) - phone 1 exchange */
    private String acsph1b;

    /** ACSPH1CI PIC X(4) - phone 1 number */
    private String acsph1c;

    /** ACSGOVTI PIC X(20) - govt ID */
    private String acsgovt;

    /** ACSPH2AI PIC X(3) - phone 2 area */
    private String acsph2a;

    /** ACSPH2BI PIC X(3) - phone 2 exchange */
    private String acsph2b;

    /** ACSPH2CI PIC X(4) - phone 2 number */
    private String acsph2c;

    /** ERRMSGI PIC X(78) */
    private String errmsg;
}
