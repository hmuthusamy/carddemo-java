package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COMEN01.CPY
 * Main Menu screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MainMenuMap extends BmsScreenMap {

    /** Option number selected by user */
    private String optnum;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
