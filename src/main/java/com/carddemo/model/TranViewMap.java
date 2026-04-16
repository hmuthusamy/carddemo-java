package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COTRN01.CPY
 * Transaction View screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TranViewMap extends BmsScreenMap {

    /** Transaction ID */
    private String tranid;

    /** Account ID */
    private String acctid;

    /** Transaction type */
    private String trantype;

    /** Transaction category */
    private String trancat;

    /** Transaction amount */
    private String tranamt;

    /** Transaction description */
    private String trandesc;

    /** Merchant name */
    private String mername;

    /** Merchant city */
    private String mercity;

    /** Origin timestamp */
    private String origts;

    /** Process timestamp */
    private String procts;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
