package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COCOM01Y.cpy
 * CARDDEMO-COMMAREA - Communication area for CardDemo application programs.
 * Not a VSAM record - commarea structure, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonArea {

    // CDEMO-GENERAL-INFO group

    /** CDEMO-FROM-TRANID PIC X(04) */
    private String cdemoFromTranid;

    /** CDEMO-FROM-PROGRAM PIC X(08) */
    private String cdemoFromProgram;

    /** CDEMO-TO-TRANID PIC X(04) */
    private String cdemoToTranid;

    /** CDEMO-TO-PROGRAM PIC X(08) */
    private String cdemoToProgram;

    /** CDEMO-USER-ID PIC X(08) */
    private String cdemoUserId;

    /** CDEMO-USER-TYPE PIC X(01) - 'A'=Admin, 'U'=User */
    private String cdemoUserType;

    /** CDEMO-PGM-CONTEXT PIC 9(01) - 0=Enter, 1=Reenter */
    private Integer cdemoPgmContext;

    // CDEMO-CUSTOMER-INFO group

    /** CDEMO-CUST-ID PIC 9(09) */
    private Long cdemoCustId;

    /** CDEMO-CUST-FNAME PIC X(25) */
    private String cdemoCustFname;

    /** CDEMO-CUST-MNAME PIC X(25) */
    private String cdemoCustMname;

    /** CDEMO-CUST-LNAME PIC X(25) */
    private String cdemoCustLname;

    // CDEMO-ACCOUNT-INFO group

    /** CDEMO-ACCT-ID PIC 9(11) */
    private Long cdemoAcctId;

    /** CDEMO-ACCT-STATUS PIC X(01) */
    private String cdemoAcctStatus;

    // CDEMO-CARD-INFO group

    /** CDEMO-CARD-NUM PIC 9(16) */
    private Long cdemoCardNum;

    // CDEMO-MORE-INFO group

    /** CDEMO-LAST-MAP PIC X(7) */
    private String cdemoLastMap;

    /** CDEMO-LAST-MAPSET PIC X(7) */
    private String cdemoLastMapset;
}
