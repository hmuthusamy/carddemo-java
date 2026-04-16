package com.carddemo.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * AdminMenuRequest – mirrors COADM1AI (BMS map input) and the COADM02Y option
 * selection field (OPTIONI in COADM1AI).
 *
 * COBOL origin:
 *   COPY COADM01.        → COADM1AI / COADM1AO  (BMS map data structures)
 *   COPY COADM02Y.       → CARDDEMO-ADMIN-MENU-OPTIONS (option table)
 *   WS-OPTION            PIC 9(02)  – parsed from OPTIONI OF COADM1AI
 */
public class AdminMenuRequest {

    /**
     * The option number typed by the user (maps to OPTIONI in COADM1AI).
     * Valid range: 1–6 (CDEMO-ADMIN-OPT-COUNT = 6 in COADM02Y).
     */
    @Min(value = 1, message = "Please enter a valid option number...")
    @Max(value = 6, message = "Please enter a valid option number...")
    private Integer optionSelected;

    /** Communication area context; mirrors LK-COMMAREA / CARDDEMO-COMMAREA. */
    private String commArea;

    // ── constructors ──────────────────────────────────────────────────────────

    public AdminMenuRequest() {}

    public AdminMenuRequest(Integer optionSelected) {
        this.optionSelected = optionSelected;
    }

    // ── accessors ─────────────────────────────────────────────────────────────

    public Integer getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(Integer optionSelected) {
        this.optionSelected = optionSelected;
    }

    public String getCommArea() {
        return commArea;
    }

    public void setCommArea(String commArea) {
        this.commArea = commArea;
    }

    @Override
    public String toString() {
        return "AdminMenuRequest{optionSelected=" + optionSelected
                + ", commArea='" + commArea + "'}";
    }
}
