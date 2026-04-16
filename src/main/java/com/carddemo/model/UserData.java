package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * UserData – JPA entity migrated from COBOL USRSEC VSAM file.
 *
 * <p>Original COBOL copybook CSUSR01Y defined the SEC-USER-DATA record:
 * <pre>
 *   01 SEC-USER-DATA.
 *     05 SEC-USR-ID    PIC X(08)  -- user identifier (key)
 *     05 SEC-USR-FNAME PIC X(20)  -- first name
 *     05 SEC-USR-LNAME PIC X(20)  -- last name
 *     05 SEC-USR-PWD   PIC X(08)  -- password (stored hashed in Java)
 *     05 SEC-USR-TYPE  PIC X(01)  -- 'A' = Admin, 'U' = Regular user
 * </pre>
 *
 * Migrated from: COUSR00C.CBL (CardDemo v1.0, CICS COBOL)
 */
@Entity
@Table(name = "users")
public class UserData {

    /** Maps to COBOL SEC-USR-ID PIC X(08) – VSAM primary key. */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    @Size(max = 8)
    private String userId;

    /** Maps to COBOL SEC-USR-FNAME PIC X(20). */
    @Column(name = "first_name", length = 20)
    @Size(max = 20)
    private String firstName;

    /** Maps to COBOL SEC-USR-LNAME PIC X(20). */
    @Column(name = "last_name", length = 20)
    @Size(max = 20)
    private String lastName;

    /**
     * Password hash. Original COBOL stored plaintext in SEC-USR-PWD PIC X(08);
     * Spring Security BCrypt hash replaces that field.
     */
    @Column(name = "password_hash", length = 72, nullable = false)
    private String passwordHash;

    /**
     * Maps to COBOL SEC-USR-TYPE PIC X(01).
     * 'A' = ADMIN (was COUSR0xC selection 'U'/'D' routing to privileged screens),
     * 'U' = regular USER.
     */
    @Column(name = "user_type", length = 1, nullable = false)
    @Size(max = 1)
    private String userType;

    /**
     * Active flag – replaces the COBOL DELETE operation (COUSR03C via 'D' flag).
     * Logical deletes keep audit history; true = active, false = deactivated.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Audit timestamp – no COBOL equivalent; added for modernization compliance. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Audit timestamp for last update. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public UserData() {
    }

    public UserData(String userId, String firstName, String lastName,
                    String passwordHash, String userType) {
        this.userId       = userId;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.passwordHash = passwordHash;
        this.userType     = userType;
        this.active       = true;
        this.createdAt    = LocalDateTime.now();
    }

    // -----------------------------------------------------------------------
    // Getters & Setters (explicit – no Lombok to keep test compilation simple)
    // -----------------------------------------------------------------------

    public String getUserId()                      { return userId; }
    public void   setUserId(String userId)         { this.userId = userId; }

    public String getFirstName()                   { return firstName; }
    public void   setFirstName(String firstName)   { this.firstName = firstName; }

    public String getLastName()                    { return lastName; }
    public void   setLastName(String lastName)     { this.lastName = lastName; }

    public String getPasswordHash()                { return passwordHash; }
    public void   setPasswordHash(String h)        { this.passwordHash = h; }

    public String getUserType()                    { return userType; }
    public void   setUserType(String userType)     { this.userType = userType; }

    public boolean isActive()                      { return active; }
    public void    setActive(boolean active)       { this.active = active; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void          setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
