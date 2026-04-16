package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVCRD01Y.cpy
 * CC-WORK-AREAS - Card work area for screen navigation and control.
 * Non-VSAM (work area / communication); no @Entity.
 * Source: CVCRD01Y.cpy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardData {

    /** CCARD-AID PIC X(5) - attention identifier */
    private String ccardAid;

    /** CCARD-NEXT-PROG PIC X(8) */
    private String ccardNextProg;

    /** CCARD-NEXT-MAPSET PIC X(7) */
    private String ccardNextMapset;

    /** CCARD-NEXT-MAP PIC X(7) */
    private String ccardNextMap;

    /** CCARD-ERROR-MSG PIC X(75) */
    private String ccardErrorMsg;

    /** CCARD-RETURN-MSG PIC X(75) */
    private String ccardReturnMsg;

    /** CC-ACCT-ID PIC X(11) */
    private String ccAcctId;

    /** CC-CARD-NUM PIC X(16) */
    private String ccCardNum;

    /** CC-CUST-ID PIC X(09) */
    private String ccCustId;
}
