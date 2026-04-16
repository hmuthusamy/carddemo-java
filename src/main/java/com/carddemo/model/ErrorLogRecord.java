package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CCPAUERY.cpy
 * ERROR-LOG-RECORD - Pending authorization error log.
 * VSAM KSDS equivalent - error log records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "error_log")
public class ErrorLogRecord {

    /** ERR-DATE PIC X(06) - Primary key (composite with ERR-TIME + ERR-PROGRAM) */
    @Id
    @Column(name = "err_date", length = 6, nullable = false)
    private String errDate;

    /** ERR-TIME PIC X(06) */
    @Column(name = "err_time", length = 6)
    private String errTime;

    /** ERR-APPLICATION PIC X(08) */
    @Column(name = "err_application", length = 8)
    private String errApplication;

    /** ERR-PROGRAM PIC X(08) */
    @Column(name = "err_program", length = 8)
    private String errProgram;

    /** ERR-LOCATION PIC X(04) */
    @Column(name = "err_location", length = 4)
    private String errLocation;

    /** ERR-LEVEL PIC X(01) - L=Log, I=Info, W=Warning, C=Critical */
    @Column(name = "err_level", length = 1)
    private String errLevel;

    /** ERR-SUBSYSTEM PIC X(01) - A=App, C=CICS, I=IMS, D=DB2, M=MQ, F=File */
    @Column(name = "err_subsystem", length = 1)
    private String errSubsystem;

    /** ERR-CODE-1 PIC X(09) */
    @Column(name = "err_code_1", length = 9)
    private String errCode1;

    /** ERR-CODE-2 PIC X(09) */
    @Column(name = "err_code_2", length = 9)
    private String errCode2;

    /** ERR-MESSAGE PIC X(50) */
    @Column(name = "err_message", length = 50)
    private String errMessage;

    /** ERR-EVENT-KEY PIC X(20) */
    @Column(name = "err_event_key", length = 20)
    private String errEventKey;
}
