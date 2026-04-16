package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CSSTRPFY.cpy
 * YYYY-STORE-PFKEY - Store PF key values into COMMAREA.
 * This copybook contains procedural COBOL logic for mapping EIBAID to PF-key codes.
 * Modelled as a key mapping holder (data-centric representation).
 * Not a VSAM record - procedure/logic copybook, no @Entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PfKeyData {

    /** Current AID value (e.g., ENTER, CLEAR, PA1, PA2, PFK01..PFK12) */
    private String currentAid;

    /** Mapped CCARD-AID value after normalization */
    private String mappedCcardAid;
}
