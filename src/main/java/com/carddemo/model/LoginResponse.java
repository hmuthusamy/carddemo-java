package com.carddemo.model;

import java.util.List;

/**
 * LoginResponse DTO – returned to the caller after successful authentication.
 *
 * COBOL equivalent context (CARDDEMO-COMMAREA fields populated on success):
 *   CDEMO-USER-ID      → username
 *   CDEMO-USER-TYPE    → userType  ("ADMIN" → ROLE_ADMIN, else → ROLE_USER)
 *   CDEMO-FROM-TRANID  → transactionId ("CC00")
 *   CDEMO-FROM-PROGRAM → programName   ("COSGN00C")
 *   CDEMO-PGM-CONTEXT  → zeros on fresh login
 *
 * The JWT token replaces the CICS COMMAREA security-context hand-off.
 */
public class LoginResponse {

    /** JWT / session token that replaces the CICS COMMAREA security context. */
    private String token;

    /** Authenticated user ID (upper-cased, mirroring COBOL FUNCTION UPPER-CASE). */
    private String username;

    /**
     * User type derived from SEC-USR-TYPE in USRSEC VSAM file.
     * Values: "ADMIN" or "USER" – used to redirect to COADM01C or COMEN01C.
     */
    private String userType;

    /** Spring Security roles mapped from RACF / USRSEC user-type. */
    private List<String> roles;

    /** Originating transaction ID – mirrors CDEMO-FROM-TRANID ("CC00"). */
    private String transactionId;

    /** Originating program – mirrors CDEMO-FROM-PROGRAM ("COSGN00C"). */
    private String programName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public LoginResponse() {}

    public LoginResponse(String token, String username, String userType,
                         List<String> roles, String transactionId, String programName) {
        this.token         = token;
        this.username      = username;
        this.userType      = userType;
        this.roles         = roles;
        this.transactionId = transactionId;
        this.programName   = programName;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }
}
