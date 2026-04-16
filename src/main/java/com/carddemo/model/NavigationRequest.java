package com.carddemo.model;

/**
 * Request payload for POST /api/menu/navigate.
 *
 * Mirrors the OPTIONI field from BMS map COMEN1AI, which the user
 * typed on the 3270 terminal (WS-OPTION in COMEN01C).
 */
public class NavigationRequest {

    /** 1-based menu option number selected by the user (was WS-OPTION). */
    private int selection;

    public NavigationRequest() {}

    public NavigationRequest(int selection) {
        this.selection = selection;
    }

    public int getSelection()               { return selection; }
    public void setSelection(int selection) { this.selection = selection; }
}
