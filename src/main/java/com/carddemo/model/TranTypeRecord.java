package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVTRA03Y.cpy
 * Data-structure for transaction type (RECLN = 60)
 * VSAM KSDS - keyed by TRAN-TYPE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tran_type")
public class TranTypeRecord {

    /** TRAN-TYPE PIC X(02) - Primary key */
    @Id
    @Column(name = "tran_type", length = 2, nullable = false)
    private String tranType;

    /** TRAN-TYPE-DESC PIC X(50) */
    @Column(name = "tran_type_desc", length = 50)
    private String tranTypeDesc;
}
