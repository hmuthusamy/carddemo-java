package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COUSR02.CPY
 * User Update screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserUpdateMap extends BmsScreenMap {

    /** User ID (read-only key) */
    private String userid;

    /** First name */
    private String fname;

    /** Last name */
    private String lname;

    /** Password */
    private String passwd;

    /** User type */
    private String usrtype;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
