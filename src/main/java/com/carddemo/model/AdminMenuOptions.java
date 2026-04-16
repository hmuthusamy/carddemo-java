package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COADM02Y.cpy
 * CARDDEMO-ADMIN-MENU-OPTIONS - Admin menu option definitions.
 * Not a VSAM record - menu configuration, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMenuOptions {

    /** CDEMO-ADMIN-OPT-COUNT PIC 9(02) */
    private Integer cdemoAdminOptCount;

    /**
     * Inner class representing a single admin menu option.
     * Corresponds to CDEMO-ADMIN-OPT OCCURS 9 TIMES.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminMenuOption {

        /** CDEMO-ADMIN-OPT-NUM PIC 9(02) */
        private Integer cdemoAdminOptNum;

        /** CDEMO-ADMIN-OPT-NAME PIC X(35) */
        private String cdemoAdminOptName;

        /** CDEMO-ADMIN-OPT-PGMNAME PIC X(08) */
        private String cdemoAdminOptPgmname;
    }

    /** CDEMO-ADMIN-OPT OCCURS 9 TIMES */
    private AdminMenuOption[] cdemoAdminOpt = new AdminMenuOption[9];
}
