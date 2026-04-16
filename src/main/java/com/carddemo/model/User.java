package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the USERS table.
 *
 * <p>Supports Spring Security authentication and role-based access control
 * for the modernised CardDemo application.
 *
 * <pre>
 * Column type mapping:
 *   user_id        BIGINT        – auto-generated surrogate PK (BIGSERIAL in DDL)
 *   username       VARCHAR(50)   – unique login name
 *   password_hash  VARCHAR(256)  – bcrypt/argon2 encoded password
 *   role           VARCHAR(20)   – Spring Security role string
 *   status         CHAR(1)       – A=active, I=inactive, L=locked
 *   first_name     VARCHAR(25)   – matches PIC X(25) width from CVCUS01Y
 *   last_name      VARCHAR(25)   – matches PIC X(25) width from CVCUS01Y
 * </pre>
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Auto-generated surrogate PK. Maps to DDL: BIGSERIAL NOT NULL */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Unique login name. Maps to DDL: VARCHAR(50) NOT NULL UNIQUE */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Bcrypt/Argon2 password hash. Maps to DDL: VARCHAR(256) NOT NULL */
    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    /** Spring Security role. Maps to DDL: VARCHAR(20) NOT NULL */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /** Account status. Maps to DDL: CHAR(1) NOT NULL – A/I/L */
    @Column(name = "status", nullable = false, length = 1)
    private String status;

    /** User first name. Maps to DDL: VARCHAR(25) NOT NULL */
    @Column(name = "first_name", nullable = false, length = 25)
    private String firstName;

    /** User last name. Maps to DDL: VARCHAR(25) NOT NULL */
    @Column(name = "last_name", nullable = false, length = 25)
    private String lastName;
}
