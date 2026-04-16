package com.carddemo.model;

/**
 * Response payload for POST /api/menu/navigate.
 *
 * Replaces the CICS {@code EXEC CICS XCTL PROGRAM(...)} construct from
 * COMEN01C.  Instead of a hard JVM transfer-of-control, the REST client
 * receives the target route and performs the redirect client-side.
 *
 * <pre>
 * COBOL equivalent (COMEN01C PROCESS-ENTER-KEY):
 *   EXEC CICS XCTL PROGRAM(CDEMO-MENU-OPT-PGMNAME(WS-OPTION))
 *             COMMAREA(CARDDEMO-COMMAREA)
 *   END-EXEC
 * </pre>
 */
public class NavigationResponse {

    /** e.g. "/api/accounts/view" — mapped from original CICS XCTL program. */
    private final String route;
    /** Human-readable label from CDEMO-MENU-OPT-NAME. */
    private final String label;
    /** Original CICS program name preserved for audit / compatibility. */
    private final String programName;
    private final String message;

    public NavigationResponse(String route, String label,
                               String programName, String message) {
        this.route       = route;
        this.label       = label;
        this.programName = programName;
        this.message     = message;
    }

    public String getRoute()       { return route;       }
    public String getLabel()       { return label;       }
    public String getProgramName() { return programName; }
    public String getMessage()     { return message;     }
}
