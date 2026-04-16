package com.carddemo.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * AdminMenuResponse – mirrors COADM1AO (BMS map output) as a REST payload.
 *
 * COBOL origin:
 *   COADM1AO fields (from COPY COADM01):
 *     TITLE01O  – CCDA-TITLE01  (application title line 1)
 *     TITLE02O  – CCDA-TITLE02  (application title line 2)
 *     TRNNAMEO  – WS-TRANID     (transaction ID, 'CA00')
 *     PGMNAMEO  – WS-PGMNAME   (program name, 'COADM01C')
 *     CURDATEO  – current date  (MM/DD/YY)
 *     CURTIMEO  – current time  (HH:MM:SS)
 *     OPTN001O … OPTN010O – option text lines
 *     ERRMSGO   – WS-MESSAGE    (error / informational message)
 *     OPTIONO   – WS-OPTION     (echo of selected option)
 *
 *   CDEMO-ADMIN-OPTIONS from COADM02Y (6 active entries):
 *     1  User List (Security)              → COUSR00C
 *     2  User Add (Security)               → COUSR01C
 *     3  User Update (Security)            → COUSR02C
 *     4  User Delete (Security)            → COUSR03C
 *     5  Transaction Type List/Update (Db2)→ COTRTLIC
 *     6  Transaction Type Maintenance (Db2)→ COTRTUPC
 */
public class AdminMenuResponse {

    // ── header fields (POPULATE-HEADER-INFO) ─────────────────────────────────

    private String title1;
    private String title2;
    private String transactionId;
    private String programName;
    private LocalDate currentDate;
    private LocalTime currentTime;

    // ── menu options (BUILD-MENU-OPTIONS) ────────────────────────────────────

    /**
     * List of up to 10 rendered option lines (OPTN001O … OPTN010O).
     * Each entry is the full formatted string, e.g. "1. User List (Security)".
     */
    private List<AdminMenuOption> menuOptions;

    // ── user-interaction fields ───────────────────────────────────────────────

    /** Echo of the selected option number (OPTIONO). */
    private Integer selectedOption;

    /** Status / error message (ERRMSGO / WS-MESSAGE). */
    private String message;

    /**
     * Maps CICS XCTL redirect intent to an HTTP navigation hint.
     * REST callers should GET /api/admin/{redirectPath} next.
     * Mirrors CDEMO-TO-PROGRAM / CDEMO-ADMIN-OPT-PGMNAME logic.
     */
    private String redirectTo;

    // ── nested DTO for a single menu item ────────────────────────────────────

    /**
     * Represents a single row from CDEMO-ADMIN-OPT (COADM02Y).
     */
    public static class AdminMenuOption {
        private int    number;   // CDEMO-ADMIN-OPT-NUM
        private String name;     // CDEMO-ADMIN-OPT-NAME
        private String program;  // CDEMO-ADMIN-OPT-PGMNAME
        private String endpointPath; // REST equivalent

        public AdminMenuOption() {}

        public AdminMenuOption(int number, String name, String program, String endpointPath) {
            this.number       = number;
            this.name         = name;
            this.program      = program;
            this.endpointPath = endpointPath;
        }

        public int    getNumber()       { return number; }
        public String getName()         { return name; }
        public String getProgram()      { return program; }
        public String getEndpointPath() { return endpointPath; }

        public void setNumber(int n)           { this.number = n; }
        public void setName(String n)          { this.name = n; }
        public void setProgram(String p)       { this.program = p; }
        public void setEndpointPath(String ep) { this.endpointPath = ep; }

        @Override
        public String toString() {
            return number + ". " + name;
        }
    }

    // ── constructors ──────────────────────────────────────────────────────────

    public AdminMenuResponse() {}

    // ── accessors ─────────────────────────────────────────────────────────────

    public String getTitle1()            { return title1; }
    public void   setTitle1(String v)    { this.title1 = v; }

    public String getTitle2()            { return title2; }
    public void   setTitle2(String v)    { this.title2 = v; }

    public String getTransactionId()     { return transactionId; }
    public void   setTransactionId(String v) { this.transactionId = v; }

    public String getProgramName()       { return programName; }
    public void   setProgramName(String v) { this.programName = v; }

    public LocalDate getCurrentDate()    { return currentDate; }
    public void      setCurrentDate(LocalDate v) { this.currentDate = v; }

    public LocalTime getCurrentTime()    { return currentTime; }
    public void      setCurrentTime(LocalTime v) { this.currentTime = v; }

    public List<AdminMenuOption> getMenuOptions()          { return menuOptions; }
    public void                  setMenuOptions(List<AdminMenuOption> v) { this.menuOptions = v; }

    public Integer getSelectedOption()       { return selectedOption; }
    public void    setSelectedOption(Integer v) { this.selectedOption = v; }

    public String getMessage()               { return message; }
    public void   setMessage(String v)       { this.message = v; }

    public String getRedirectTo()            { return redirectTo; }
    public void   setRedirectTo(String v)    { this.redirectTo = v; }
}
