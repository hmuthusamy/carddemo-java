package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSUSR01Y.cpy
 * SEC-USER-DATA - Security user data record.
 * VSAM KSDS - keyed by SEC-USR-ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sec_user")
public class UserData {

    /** SEC-USR-ID PIC X(08) - Primary key */
    @Id
    @Column(name = "sec_usr_id", length = 8, nullable = false)
    private String secUsrId;

    /** SEC-USR-FNAME PIC X(20) */
    @Column(name = "sec_usr_fname", length = 20)
    private String secUsrFname;

    /** SEC-USR-LNAME PIC X(20) */
    @Column(name = "sec_usr_lname", length = 20)
    private String secUsrLname;

    /** SEC-USR-PWD PIC X(08) */
    @Column(name = "sec_usr_pwd", length = 8)
    private String secUsrPwd;

    /** SEC-USR-TYPE PIC X(01) */
    @Column(name = "sec_usr_type", length = 1)
    private String secUsrType;

    /** SEC-USR-FILLER PIC X(23) */
    @Column(name = "sec_usr_filler", length = 23)
    private String secUsrFiller;
}
