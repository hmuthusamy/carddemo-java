package com.carddemo.service;

import com.carddemo.model.MenuItem;
import com.carddemo.model.NavigationResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MenuService — business logic layer for the main menu.
 *
 * <p>Migrated from COMEN01C.CBL + COMEN02Y.cpy (CardDemo v1.0).
 *
 * <p>The original COBOL stored menu options in the CDEMO-MENU-OPTIONS-DATA
 * table (COMEN02Y copybook).  Each entry had:
 * <ul>
 *   <li>Option number  (PIC 9(02))</li>
 *   <li>Display name   (PIC X(35))</li>
 *   <li>CICS program   (PIC X(08))  — XCTL target</li>
 *   <li>User type      (PIC X(01))  — 'U'=all users, 'A'=admin only</li>
 * </ul>
 *
 * <p>The CICS {@code EXEC CICS XCTL PROGRAM(...)} transfer-of-control is
 * replaced by returning a REST route string to the caller
 * ({@link NavigationResponse#getRoute()}).
 */
@Service
public class MenuService {

    // ----------------------------------------------------------------
    // Menu option registry — mirrors COMEN02Y CDEMO-MENU-OPTIONS-DATA
    // Key: option number (1-based), Value: MenuItem
    // ----------------------------------------------------------------
    private static final Map<Integer, MenuItem> MENU_OPTIONS;

    static {
        MENU_OPTIONS = new LinkedHashMap<>();
        register(1,  "Account View",               "COACTVWC", "U");
        register(2,  "Account Update",             "COACTUPC", "U");
        register(3,  "Credit Card List",            "COCRDLIC", "U");
        register(4,  "Credit Card View",            "COCRDSLC", "U");
        register(5,  "Credit Card Update",          "COCRDUPC", "U");
        register(6,  "Transaction List",            "COTRN00C", "U");
        register(7,  "Transaction View",            "COTRN01C", "U");
        register(8,  "Transaction Add",             "COTRN02C", "U");
        register(9,  "Transaction Reports",         "CORPT00C", "U");
        register(10, "Bill Payment",                "COBIL00C", "U");
        register(11, "Pending Authorization View",  "COPAUS0C", "U");
        // Admin-only options (CDEMO-MENU-OPT-USRTYPE = 'A')
        register(12, "User Management",            "COUSR00C", "A");
    }

    private static void register(int num, String name, String pgm, String type) {
        MENU_OPTIONS.put(num, new MenuItem(num, name, pgm, type));
    }

    // ----------------------------------------------------------------
    // CICS XCTL target → REST route mapping
    // Replaces the hard program transfer with a navigable REST path
    // ----------------------------------------------------------------
    private static final Map<String, String> PROGRAM_TO_ROUTE = Map.ofEntries(
            Map.entry("COACTVWC", "/api/accounts/view"),
            Map.entry("COACTUPC", "/api/accounts/update"),
            Map.entry("COCRDLIC", "/api/cards/list"),
            Map.entry("COCRDSLC", "/api/cards/view"),
            Map.entry("COCRDUPC", "/api/cards/update"),
            Map.entry("COTRN00C", "/api/transactions/list"),
            Map.entry("COTRN01C", "/api/transactions/view"),
            Map.entry("COTRN02C", "/api/transactions/add"),
            Map.entry("CORPT00C", "/api/reports/transactions"),
            Map.entry("COBIL00C", "/api/payments/bill"),
            Map.entry("COPAUS0C", "/api/authorizations/pending"),
            Map.entry("COUSR00C", "/api/admin/users"),
            Map.entry("COSGN00C", "/api/auth/signout")
    );

    /**
     * Returns menu items visible to the authenticated user.
     *
     * <p>Mirrors COBOL logic (COMEN01C PROCESS-ENTER-KEY):
     * <pre>
     *   IF CDEMO-USRTYP-USER AND
     *      CDEMO-MENU-OPT-USRTYPE(WS-OPTION) = 'A'
     *       ... No access - Admin Only option
     *   END-IF
     * </pre>
     *
     * @param isAdmin {@code true} when the caller has ROLE_ADMIN
     * @return filtered, ordered list of available menu items
     */
    public List<MenuItem> getMenuItemsForRole(boolean isAdmin) {
        return MENU_OPTIONS.values().stream()
                .filter(item -> isAdmin || !item.isAdminOnly())
                .collect(Collectors.toList());
    }

    /**
     * Resolves a menu selection to a navigation response.
     *
     * <p>Replicates COMEN01C PROCESS-ENTER-KEY logic:
     * <ol>
     *   <li>Validate option is numeric and within bounds</li>
     *   <li>Guard admin-only options for non-admin users</li>
     *   <li>Return the REST route equivalent of the CICS XCTL target</li>
     * </ol>
     *
     * @param selection 1-based option number (was WS-OPTION in COBOL)
     * @param isAdmin   whether caller has ROLE_ADMIN
     * @return NavigationResponse carrying the REST route (replaces XCTL)
     * @throws IllegalArgumentException if selection is invalid
     * @throws SecurityException        if a non-admin selects admin-only item
     */
    public NavigationResponse navigate(int selection, boolean isAdmin) {
        // Validation: "Please enter a valid option number..."
        if (selection < 1 || selection > MENU_OPTIONS.size()) {
            throw new IllegalArgumentException(
                    "Please enter a valid option number (1-" +
                    MENU_OPTIONS.size() + ").");
        }

        MenuItem item = MENU_OPTIONS.get(selection);
        if (item == null) {
            throw new IllegalArgumentException(
                    "Please enter a valid option number.");
        }

        // COBOL: "No access - Admin Only option..."
        if (item.isAdminOnly() && !isAdmin) {
            throw new SecurityException(
                    "No access - Admin Only option: " + item.getDisplayName());
        }

        String route = resolveRoute(item.getProgramName());

        return new NavigationResponse(
                route,
                item.getDisplayName(),
                item.getProgramName(),
                "Navigating to: " + item.getDisplayName());
    }

    /**
     * Resolves a CICS XCTL program name to a Spring REST route.
     *
     * <p>Replaces {@code EXEC CICS XCTL PROGRAM(...) END-EXEC}.
     * Unknown programs fall back to a generic route rather than
     * throwing (mirrors COBOL 'coming soon' path for DUMMY* programs).
     *
     * @param programName 8-char CICS program name
     * @return corresponding REST API path
     */
    public String resolveRoute(String programName) {
        if (programName == null) return "/api/menu";
        // Mirror: WHEN CDEMO-MENU-OPT-PGMNAME(WS-OPTION)(1:5) = 'DUMMY'
        if (programName.trim().toUpperCase().startsWith("DUMMY")) {
            return "/api/menu/coming-soon";
        }
        return PROGRAM_TO_ROUTE.getOrDefault(
                programName.trim().toUpperCase(), "/api/menu");
    }

    /** Exposed for testing — total number of registered menu options. */
    public int getMenuOptionCount() {
        return MENU_OPTIONS.size();
    }
}
