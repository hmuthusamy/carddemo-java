package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL BMS copybook COUSR03.CPY
 * User Delete screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserDeleteMap extends BmsScreenMap {

    /** User ID */
    private String userid;

    /** Confirm delete flag */
    private String confirm;

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
