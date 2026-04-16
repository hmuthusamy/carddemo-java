package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COTRN02.CPY
 * Transaction Add screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TranAddMap extends BmsScreenMap {

    /** Card number */
    private String cardnum;

    /** Transaction type */
    private String trantype;

    /** Transaction category */
    private String trancat;

    /** Transaction source */
    private String transrc;

    /** Transaction description */
    private String trandesc;

    /** Transaction amount */
    private String tranamt;

    /** Merchant ID */
    private String merid;

    /** Merchant name */
    private String mername;

    /** Merchant city */
    private String mercity;

    /** Merchant zip */
    private String merzip;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
