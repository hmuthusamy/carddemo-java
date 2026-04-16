package com.carddemo.model;

/**
 * Represents a single menu option migrated from COMEN02Y copybook.
 * Each entry maps to a CDEMO-MENU-OPT entry in the original COBOL.
 *
 * <pre>
 * COBOL source (COMEN02Y.cpy):
 *   15 CDEMO-MENU-OPT-NUM           PIC 9(02).
 *   15 CDEMO-MENU-OPT-NAME          PIC X(35).
 *   15 CDEMO-MENU-OPT-PGMNAME       PIC X(08).
 *   15 CDEMO-MENU-OPT-USRTYPE       PIC X(01).  -- 'U'=user, 'A'=admin
 * </pre>
 */
public class MenuItem {

    private final int optionNumber;
    private final String displayName;
    /** Original CICS XCTL target program name, mapped to REST route. */
    private final String programName;
    /** 'U' = regular user, 'A' = admin only. */
    private final String userType;

    public MenuItem(int optionNumber, String displayName,
                    String programName, String userType) {
        this.optionNumber = optionNumber;
        this.displayName  = displayName;
        this.programName  = programName;
        this.userType     = userType;
    }

    public int getOptionNumber()  { return optionNumber; }
    public String getDisplayName(){ return displayName;  }
    public String getProgramName(){ return programName;  }
    public String getUserType()   { return userType;     }

    /** Returns true when this option is restricted to ADMIN role. */
    public boolean isAdminOnly()  { return "A".equalsIgnoreCase(userType); }
}
