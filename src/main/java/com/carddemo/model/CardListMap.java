package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

/**
 * Java model for COBOL BMS copybook COCRDLI.CPY
 * Credit Card List screen map.
 * Not a VSAM record - CICS BMS screen map, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CardListMap extends BmsScreenMap {

    /** Account ID filter */
    private String acctid;

    /** Page number */
    private String pageno;

    /** Card rows displayed on screen */
    private List<CardRow> cardRows = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
    public static class CardRow {
        private String sel;
        private String cardNum;
        private String cardStatus;
        private String cardExpiry;
    }

    /** ERRMSGI PIC X(78) */
    private String errMsg;
}
