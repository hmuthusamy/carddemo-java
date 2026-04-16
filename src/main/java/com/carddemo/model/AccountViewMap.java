package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COACTVW.CPY
 * Account View screen map (COACTVWAI).
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AccountViewMap extends BmsScreenMap {

    /** ACCTSIDI PIC X(11) - account ID */
    private String acctsid;

    /** ACSTTUSI PIC X(1) - account status */
    private String acsttus;

    /** ACRDLIMI PIC X(15) - credit limit */
    private String acrdlim;

    /** ACURBALI PIC X(15) - current balance */
    private String acurbal;

    /** ACSTNUMI PIC X(9) - customer number */
    private String acstnum;

    /** ACSFNAMI PIC X(25) - first name */
    private String acsfnam;

    /** ACSMNIMI PIC X(25) - middle name */
    private String acsmnam;

    /** ACSLNAMI PIC X(25) - last name */
    private String acslnam;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
