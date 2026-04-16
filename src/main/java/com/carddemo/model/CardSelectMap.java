package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COCRDSL.CPY
 * Credit Card Select/View screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CardSelectMap extends BmsScreenMap {

    /** CARDNUMI PIC X(16) */
    private String cardnum;

    /** ACCTIDI PIC X(11) */
    private String acctid;

    /** EMBOSSED name */
    private String embname;

    /** Card status */
    private String cardstat;

    /** Card expiry */
    private String cardexp;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
