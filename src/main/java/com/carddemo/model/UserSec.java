package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JPA entity mapping to the USRSEC VSAM file from COBOL COUSR02C.
 *
 * COBOL copybook fields (CSUSR01Y):
 *   SEC-USR-ID    PIC X(08)
 *   SEC-USR-FNAME PIC X(20)
 *   SEC-USR-LNAME PIC X(20)
 *   SEC-USR-PWD   PIC X(08)
 *   SEC-USR-TYPE  PIC X(01)  – 'R'=regular, 'A'=admin
 */
@Entity
@Table(name = "usrsec")
public class UserSec {

    /** Maps to SEC-USR-ID PIC X(08) – RIDFLD (primary key). */
    @Id
    @Column(name = "usr_id", length = 8, nullable = false)
    @NotBlank(message = "User ID can NOT be empty")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String usrId;

    /** Maps to SEC-USR-FNAME PIC X(20). */
    @Column(name = "usr_fname", length = 20, nullable = false)
    @NotBlank(message = "First Name can NOT be empty")
    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String usrFname;

    /** Maps to SEC-USR-LNAME PIC X(20). */
    @Column(name = "usr_lname", length = 20, nullable = false)
    @NotBlank(message = "Last Name can NOT be empty")
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String usrLname;

    /** Maps to SEC-USR-PWD PIC X(08). */
    @Column(name = "usr_pwd", length = 8, nullable = false)
    @NotBlank(message = "Password can NOT be empty")
    @Size(max = 8, message = "Password must not exceed 8 characters")
    private String usrPwd;

    /**
     * Maps to SEC-USR-TYPE PIC X(01).
     * Valid values: 'R' (regular user) or 'A' (admin).
     */
    @Column(name = "usr_type", length = 1, nullable = false)
    @NotBlank(message = "User Type can NOT be empty")
    @Size(max = 1, message = "User Type must be a single character")
    private String usrType;

    // ---------------------------------------------------------------
    // No-arg constructor required by JPA
    // ---------------------------------------------------------------
    public UserSec() {}

    public UserSec(String usrId, String usrFname, String usrLname,
                   String usrPwd, String usrType) {
        this.usrId    = usrId;
        this.usrFname = usrFname;
        this.usrLname = usrLname;
        this.usrPwd   = usrPwd;
        this.usrType  = usrType;
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------
    public String getUsrId()    { return usrId; }
    public void   setUsrId(String usrId) { this.usrId = usrId; }

    public String getUsrFname() { return usrFname; }
    public void   setUsrFname(String usrFname) { this.usrFname = usrFname; }

    public String getUsrLname() { return usrLname; }
    public void   setUsrLname(String usrLname) { this.usrLname = usrLname; }

    public String getUsrPwd()   { return usrPwd; }
    public void   setUsrPwd(String usrPwd) { this.usrPwd = usrPwd; }

    public String getUsrType()  { return usrType; }
    public void   setUsrType(String usrType) { this.usrType = usrType; }
}
