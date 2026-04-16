package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

/**
 * Java model for COBOL BMS copybook COUSR00.CPY
 * User List screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserListMap extends BmsScreenMap {

    private String pageno;

    private List<UserRow> userRows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
    public static class UserRow {
        private String sel;
        private String userId;
        private String fname;
        private String lname;
        private String usrtype;
    }

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
