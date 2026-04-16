package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the USRSEC VSAM file used by COUSR03C.
 *
 * <p>COBOL VSAM key field: SEC-USR-ID (PIC X(08)) → {@link #userId}
 * <p>COBOL data fields:
 * <ul>
 *   <li>SEC-USR-FNAME (PIC X(20))  → {@link #firstName}</li>
 *   <li>SEC-USR-LNAME (PIC X(20))  → {@link #lastName}</li>
 *   <li>SEC-USR-TYPE  (PIC X(01))  → {@link #userType}</li>
 *   <li>SEC-USR-PWD   (PIC X(08))  → {@link #password}</li>
 * </ul>
 */
@Entity
@Table(name = "usrsec")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Primary key — maps to COBOL SEC-USR-ID (PIC X(08)). */
    @Id
    @Column(name = "usr_id", length = 8, nullable = false)
    private String userId;

    /** Maps to COBOL SEC-USR-FNAME (PIC X(20)). */
    @Column(name = "usr_fname", length = 20)
    private String firstName;

    /** Maps to COBOL SEC-USR-LNAME (PIC X(20)). */
    @Column(name = "usr_lname", length = 20)
    private String lastName;

    /**
     * Maps to COBOL SEC-USR-TYPE (PIC X(01)).
     * Value 'A' = admin, 'U' = regular user.
     */
    @Column(name = "usr_type", length = 1)
    private String userType;

    /** Maps to COBOL SEC-USR-PWD (PIC X(08)). */
    @Column(name = "usr_pwd", length = 8)
    private String password;

    /**
     * Migration extension: replaces physical VSAM DELETE with logical status.
     * Set to INACTIVE by {@link com.carddemo.service.UserDeleteService}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usr_status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Audit timestamp: set when soft-delete is performed.
     * Not present in the original COBOL record — added for traceability.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
