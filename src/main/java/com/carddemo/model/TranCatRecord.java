package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVTRA04Y.cpy
 * Data-structure for transaction category type (RECLN = 60)
 * VSAM KSDS - composite key (TRAN-TYPE-CD + TRAN-CAT-CD)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tran_cat")
public class TranCatRecord {

    /** TRAN-TYPE-CD PIC X(02) - part of composite key */
    @Id
    @Column(name = "tran_type_cd", length = 2, nullable = false)
    private String tranTypeCd;

    /** TRAN-CAT-CD PIC 9(04) - part of composite key */
    @Column(name = "tran_cat_cd", precision = 4, nullable = false)
    private Integer tranCatCd;

    /** TRAN-CAT-TYPE-DESC PIC X(50) */
    @Column(name = "tran_cat_type_desc", length = 50)
    private String tranCatTypeDesc;
}
