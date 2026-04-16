package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COSGN00.CPY
 * Sign-On screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SignOnMap extends BmsScreenMap {

    /** USERIDI PIC X(8) - user ID */
    private String userid;

    /** PASSWDI PIC X(8) - password (masked) */
    private String passwd;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
