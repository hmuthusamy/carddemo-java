package com.carddemo.service;

import com.carddemo.model.AdminMenuRequest;
import com.carddemo.model.AdminMenuResponse;
import com.carddemo.model.AdminMenuResponse.AdminMenuOption;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminService – business logic migrated from COADM01C paragraphs.
 *
 * COBOL paragraph → Java method mapping
 * ──────────────────────────────────────────────────────────────────────────
 * POPULATE-HEADER-INFO   → populateHeaderInfo(AdminMenuResponse)
 * BUILD-MENU-OPTIONS     → buildMenuOptions()  returns List<AdminMenuOption>
 * PROCESS-ENTER-KEY      → processEnterKey(AdminMenuRequest) → AdminMenuResponse
 * RETURN-TO-SIGNON-SCREEN → getSignonRedirect()              → String (path)
 * PGMIDERR-ERR-PARA      → buildNotInstalledResponse(int)    → AdminMenuResponse
 *
 * Menu options come from COADM02Y (CDEMO-ADMIN-OPT-COUNT = 6):
 *   1  User List (Security)               COUSR00C  →  /api/users
 *   2  User Add (Security)                COUSR01C  →  /api/users/add
 *   3  User Update (Security)             COUSR02C  →  /api/users/update
 *   4  User Delete (Security)             COUSR03C  →  /api/users/delete
 *   5  Transaction Type List/Update (Db2) COTRTLIC  →  /api/transactions/types
 *   6  Transaction Type Maintenance (Db2) COTRTUPC  →  /api/transactions/maintenance
 */
@Service
public class AdminService {

    // ── constants that mirror WORKING-STORAGE / COADM02Y constants ────────────

    public static final String PROGRAM_NAME      = "COADM01C";
    public static final String TRANSACTION_ID   = "CA00";
    public static final String TITLE1           = "AWS CardDemo";
    public static final String TITLE2           = "Admin Menu";
    public static final int    OPT_COUNT        = 6;           // CDEMO-ADMIN-OPT-COUNT
    public static final String SIGNON_PROGRAM   = "COSGN00C";
    public static final String SIGNON_PATH      = "/api/auth/signon";
    public static final String MSG_INVALID_KEY  = "Please enter a valid option number...";
    public static final String MSG_NOT_INSTALLED = "This option is not installed ...";

    /**
     * Definition table – mirrors CARDDEMO-ADMIN-MENU-OPTIONS from COADM02Y.
     * Columns: optNum, optName(35 chars), pgmName(8 chars), restPath
     */
    private static final Object[][] OPTION_TABLE = {
        { 1, "User List (Security)",               "COUSR00C", "/api/users" },
        { 2, "User Add (Security)",                "COUSR01C", "/api/users/add" },
        { 3, "User Update (Security)",             "COUSR02C", "/api/users/update" },
        { 4, "User Delete (Security)",             "COUSR03C", "/api/users/delete" },
        { 5, "Transaction Type List/Update (Db2)", "COTRTLIC", "/api/transactions/types" },
        { 6, "Transaction Type Maintenance (Db2)", "COTRTUPC", "/api/transactions/maintenance" }
    };

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Builds the initial (send) admin menu response.
     * Maps: SEND-MENU-SCREEN → POPULATE-HEADER-INFO + BUILD-MENU-OPTIONS.
     */
    public AdminMenuResponse buildMenuScreen() {
        AdminMenuResponse resp = new AdminMenuResponse();
        populateHeaderInfo(resp);
        resp.setMenuOptions(buildMenuOptions());
        return resp;
    }

    /**
     * Processes a user option selection.
     * Maps: PROCESS-ENTER-KEY paragraph.
     *
     * Validation rules (from COBOL):
     *   • WS-OPTION IS NOT NUMERIC  → invalid
     *   • WS-OPTION > CDEMO-ADMIN-OPT-COUNT → invalid
     *   • WS-OPTION = ZEROS         → invalid
     *   • CDEMO-ADMIN-OPT-PGMNAME starts with 'DUMMY' → not installed
     *
     * @param request incoming option selection (RECEIVE-MENU-SCREEN equivalent)
     * @return populated AdminMenuResponse (SEND-MENU-SCREEN equivalent)
     */
    public AdminMenuResponse processEnterKey(AdminMenuRequest request) {
        AdminMenuResponse resp = new AdminMenuResponse();
        populateHeaderInfo(resp);
        resp.setMenuOptions(buildMenuOptions());

        int option = (request.getOptionSelected() != null) ? request.getOptionSelected() : 0;
        resp.setSelectedOption(option);

        // ── validation mirrors COBOL IF block ──
        if (option < 1 || option > OPT_COUNT) {
            resp.setMessage(MSG_INVALID_KEY);
            return resp;
        }

        // ── DUMMY guard (PGMIDERR equivalent) ──
        String pgmName = (String) OPTION_TABLE[option - 1][2];
        if (pgmName.toUpperCase().startsWith("DUMMY")) {
            resp.setMessage(MSG_NOT_INSTALLED);
            return resp;
        }

        // ── success: set redirect (mirrors EXEC CICS XCTL PROGRAM(…)) ──
        String restPath = (String) OPTION_TABLE[option - 1][3];
        resp.setRedirectTo(restPath);
        resp.setMessage("");
        return resp;
    }

    /**
     * Returns the REST path for the signon screen.
     * Maps: RETURN-TO-SIGNON-SCREEN paragraph.
     */
    public String getSignonRedirect() {
        return SIGNON_PATH;
    }

    /**
     * Builds an error response for PF3 key or session-less entry.
     * Convenience factory used by the controller.
     */
    public AdminMenuResponse buildSignonRedirectResponse() {
        AdminMenuResponse resp = new AdminMenuResponse();
        populateHeaderInfo(resp);
        resp.setMenuOptions(buildMenuOptions());
        resp.setRedirectTo(SIGNON_PATH);
        resp.setMessage("Session expired. Please sign in again.");
        return resp;
    }

    /**
     * Builds an "option not installed" error response.
     * Maps: PGMIDERR-ERR-PARA.
     */
    public AdminMenuResponse buildNotInstalledResponse(int option) {
        AdminMenuResponse resp = new AdminMenuResponse();
        populateHeaderInfo(resp);
        resp.setMenuOptions(buildMenuOptions());
        resp.setSelectedOption(option);
        resp.setMessage(MSG_NOT_INSTALLED);
        return resp;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Populates header fields.
     * Maps: POPULATE-HEADER-INFO paragraph.
     */
    void populateHeaderInfo(AdminMenuResponse resp) {
        resp.setTitle1(TITLE1);
        resp.setTitle2(TITLE2);
        resp.setTransactionId(TRANSACTION_ID);
        resp.setProgramName(PROGRAM_NAME);
        resp.setCurrentDate(LocalDate.now());
        resp.setCurrentTime(LocalTime.now());
    }

    /**
     * Builds all menu option objects.
     * Maps: BUILD-MENU-OPTIONS paragraph (EVALUATE WS-IDX WHEN 1 … WHEN 6).
     */
    List<AdminMenuOption> buildMenuOptions() {
        List<AdminMenuOption> opts = new ArrayList<>();
        for (Object[] row : OPTION_TABLE) {
            int    num  = (Integer) row[0];
            String name = (String)  row[1];
            String pgm  = (String)  row[2];
            String path = (String)  row[3];
            opts.add(new AdminMenuOption(num, name, pgm, path));
        }
        return opts;
    }
}
