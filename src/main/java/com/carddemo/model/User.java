package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a CardDemo user record.
 *
 * <p>Maps to the USRSEC VSAM file described in CSUSR01Y.cpy:
 * <pre>
 *   01 SEC-USER-DATA.
 *     05 SEC-USR-ID     PIC X(08)   → id
 *     05 SEC-USR-FNAME  PIC X(20)   → firstName
 *     05 SEC-USR-LNAME  PIC X(20)   → lastName
 *     05 SEC-USR-PWD    PIC X(08)   → password  (BCrypt-hashed in Java)
 *     05 SEC-USR-TYPE   PIC X(01)   → userType  ('A'=admin, 'U'=user — COCOM01Y.cpy)
 * </pre>
 */
@Entity
@Table(name = "users")
public class User {

    /** Maps to SEC-USR-ID (PIC X(08)). */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    /** Maps to SEC-USR-FNAME (PIC X(20)). */
    @Column(name = "first_name", length = 20)
    private String firstName;

    /** Maps to SEC-USR-LNAME (PIC X(20)). */
    @Column(name = "last_name", length = 20)
    private String lastName;

    /**
     * Maps to SEC-USR-PWD (PIC X(08)).
     * Stored as a BCrypt hash — the original 8-char plain-text value is NOT retained.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Maps to SEC-USR-TYPE (PIC X(01)).
     * <ul>
     *   <li>{@code 'A'} — CDEMO-USRTYP-ADMIN → {@code ROLE_ADMIN}</li>
     *   <li>{@code 'U'} — CDEMO-USRTYP-USER  → {@code ROLE_USER}</li>
     * </ul>
     */
    @Column(name = "user_type", length = 1, nullable = false)
    private String userType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected User() {
        // JPA
    }

    public User(String userId, String firstName, String lastName,
                String password, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getUserId()    { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getPassword()  { return password; }
    public String getUserType()  { return userType; }

    public void setUserId(String userId)       { this.userId = userId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName)   { this.lastName = lastName; }
    public void setPassword(String password)   { this.password = password; }
    public void setUserType(String userType)   { this.userType = userType; }
}
