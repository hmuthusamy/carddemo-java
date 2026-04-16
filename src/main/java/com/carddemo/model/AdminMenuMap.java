package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COADM01.CPY
 * Admin Menu screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AdminMenuMap extends BmsScreenMap {

    /** Screen option selection field */
    private String optionSelected;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
