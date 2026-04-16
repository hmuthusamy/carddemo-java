package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybooks in app/cpy-bms/:
 * - COACTUP.CPY  → Account Update screen map (CACTUPAI)
 * - COACTVW.CPY  → Account View screen map
 * - COADM01.CPY  → Admin screen map
 * - COBIL00.CPY  → Bill Payment screen map
 * - COCRDLI.CPY  → Credit Card List screen map
 * - COCRDSL.CPY  → Credit Card Select screen map
 * - COCRDUP.CPY  → Credit Card Update screen map
 * - COMEN01.CPY  → Main Menu screen map
 * - CORPT00.CPY  → Report screen map
 * - COSGN00.CPY  → Sign-On screen map
 * - COTRN00.CPY  → Transaction List screen map
 * - COTRN01.CPY  → Transaction View screen map
 * - COTRN02.CPY  → Transaction Add screen map
 * - COUSR00.CPY  → User List screen map
 * - COUSR01.CPY  → User Add screen map
 * - COUSR02.CPY  → User Update screen map
 * - COUSR03.CPY  → User Delete screen map
 *
 * All are CICS BMS screen maps (I/O maps).
 * Not VSAM records - screen I/O structures, no @Entity.
 * This class provides a unified base for common BMS screen header fields
 * shared across all map structures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsScreenMap {

    /** TRNNAMEI PIC X(4) - transaction name */
    private String trnname;

    /** TITLE01I PIC X(40) - screen title line 1 */
    private String title01;

    /** CURDATEI PIC X(8) - current date */
    private String curdate;

    /** PGMNAMEI PIC X(8) - program name */
    private String pgmname;

    /** TITLE02I PIC X(40) - screen title line 2 */
    private String title02;

    /** CURTIMEI PIC X(8) - current time */
    private String curtime;

    /** ERRMSGI PIC X(78) - error message */
    private String errmsg;
}
