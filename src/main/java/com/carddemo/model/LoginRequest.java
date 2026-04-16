package com.carddemo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * LoginRequest DTO – mirrors the COSGN00C COBOL sign-on screen fields:
 *   USERIDI (PIC X(08))  → username (max 8 chars, upper-cased by service layer)
 *   PASSWDI (PIC X(08))  → password (max 8 chars)
 *
 * COBOL validation rules preserved:
 *   - User ID must not be blank  ("Please enter User ID ...")
 *   - Password must not be blank ("Please enter Password ...")
 */
public class LoginRequest {

    /** Maps to WS-USER-ID / USERIDI OF COSGN0AI (PIC X(08)). */
    @NotBlank(message = "Please enter User ID ...")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String username;

    /** Maps to WS-USER-PWD / PASSWDI OF COSGN0AI (PIC X(08)). */
    @NotBlank(message = "Please enter Password ...")
    @Size(max = 8, message = "Password must not exceed 8 characters")
    private String password;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
