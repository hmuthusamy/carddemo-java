package com.carddemo.model;

import java.util.List;

/**
 * Response payload for GET /api/menu.
 * Carries the list of menu items visible to the requesting user.
 */
public class MenuResponse {

    private final List<MenuItem> menuItems;
    private final String currentUser;

    public MenuResponse(List<MenuItem> menuItems, String currentUser) {
        this.menuItems   = menuItems;
        this.currentUser = currentUser;
    }

    public List<MenuItem> getMenuItems() { return menuItems;   }
    public String getCurrentUser()       { return currentUser; }
}
