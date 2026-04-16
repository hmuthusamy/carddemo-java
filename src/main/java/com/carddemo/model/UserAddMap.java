package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COUSR01.CPY
 * User Add screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserAddMap extends BmsScreenMap {

    /** User ID */
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
