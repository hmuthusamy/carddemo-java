package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

/**
 * Java model for COBOL BMS copybook COTRN00.CPY
 * Transaction List screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TranListMap extends BmsScreenMap {

    /** Account ID filter */
    private String acctid;

    /** Page number */
    private String pageno;

    /** Transaction rows */
    private List<TranRow> tranRows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
    public static class TranRow {
        private String sel;
        private String tranId;
        private String tranType;
        private String tranAmt;
        private String tranDate;
    }

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
