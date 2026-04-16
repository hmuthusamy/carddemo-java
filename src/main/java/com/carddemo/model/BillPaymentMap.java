package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COBIL00.CPY
 * Bill Payment screen map (CBIL00AI).
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BillPaymentMap extends BmsScreenMap {

    /** Account ID input field */
    private String acctid;

    /** Payment amount */
    private String payAmt;

    /** Confirmation field */
    private String confirm;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
