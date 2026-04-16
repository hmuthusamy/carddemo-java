package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook CORPT00.CPY
 * Transaction Report screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ReportMap extends BmsScreenMap {

    /** Report start date */
    private String startDate;

    /** Report end date */
    private String endDate;

    /** Account ID filter */
    private String acctid;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
