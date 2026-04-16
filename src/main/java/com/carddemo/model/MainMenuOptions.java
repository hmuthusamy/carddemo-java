package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook COMEN02Y.cpy
 * CARDDEMO-MAIN-MENU-OPTIONS - Main menu option definitions.
 * Not a VSAM record - menu configuration, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainMenuOptions {

    /** CDEMO-MENU-OPT-COUNT PIC 9(02) */
    private Integer cdemoMenuOptCount;

    /**
     * Inner class representing a single menu option.
     * Corresponds to CDEMO-MENU-OPT OCCURS 12 TIMES.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuOption {

        /** CDEMO-MENU-OPT-NUM PIC 9(02) */
        private Integer cdemoMenuOptNum;

        /** CDEMO-MENU-OPT-NAME PIC X(35) */
        private String cdemoMenuOptName;

        /** CDEMO-MENU-OPT-PGMNAME PIC X(08) */
        private String cdemoMenuOptPgmname;

        /** CDEMO-MENU-OPT-USRTYPE PIC X(01) */
        private String cdemoMenuOptUsrtype;
    }

    /** CDEMO-MENU-OPT OCCURS 12 TIMES */
    private MenuOption[] cdemoMenuOpt = new MenuOption[12];
}
