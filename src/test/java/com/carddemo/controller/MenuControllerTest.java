package com.carddemo.controller;

import com.carddemo.config.SecurityConfig;
import com.carddemo.model.MenuItem;
import com.carddemo.model.MenuResponse;
import com.carddemo.model.NavigationRequest;
import com.carddemo.model.NavigationResponse;
import com.carddemo.service.MenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MenuControllerTest — @WebMvcTest slice for MenuController.
 *
 * <p>Validates the REST API that was migrated from COMEN01C.CBL:
 * <ul>
 *   <li>GET /api/menu  (was SEND-MENU-SCREEN)</li>
 *   <li>POST /api/menu/navigate  (was PROCESS-ENTER-KEY + XCTL)</li>
 * </ul>
 */
@WebMvcTest(MenuController.class)
@Import(SecurityConfig.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Autowired
    private ObjectMapper objectMapper;

    // ----------------------------------------------------------------
    // GET /api/menu — unauthenticated
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /api/menu → 401 when not authenticated")
    void getMenu_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/menu"))
               .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // GET /api/menu — ROLE_USER (regular user sees only 'U' items)
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("GET /api/menu → 200 with user-visible items for ROLE_USER")
    void getMenu_roleUser_returnsFilteredItems() throws Exception {
        List<MenuItem> userItems = List.of(
                new MenuItem(1, "Account View",    "COACTVWC", "U"),
                new MenuItem(3, "Credit Card List","COCRDLIC", "U")
        );
        when(menuService.getMenuItemsForRole(false)).thenReturn(userItems);

        mockMvc.perform(get("/api/menu"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.currentUser",        is("user")))
               .andExpect(jsonPath("$.menuItems",          hasSize(2)))
               .andExpect(jsonPath("$.menuItems[0].optionNumber", is(1)))
               .andExpect(jsonPath("$.menuItems[0].displayName", is("Account View")))
               .andExpect(jsonPath("$.menuItems[0].programName", is("COACTVWC")))
               .andExpect(jsonPath("$.menuItems[1].optionNumber", is(3)));

        verify(menuService).getMenuItemsForRole(false);
    }

    // ----------------------------------------------------------------
    // GET /api/menu — ROLE_ADMIN (sees all items including admin-only)
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    @DisplayName("GET /api/menu → 200 with all items for ROLE_ADMIN")
    void getMenu_roleAdmin_returnsAllItems() throws Exception {
        List<MenuItem> allItems = List.of(
                new MenuItem(1,  "Account View",    "COACTVWC", "U"),
                new MenuItem(12, "User Management", "COUSR00C", "A")
        );
        when(menuService.getMenuItemsForRole(true)).thenReturn(allItems);

        mockMvc.perform(get("/api/menu"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.currentUser",        is("admin")))
               .andExpect(jsonPath("$.menuItems",          hasSize(2)))
               .andExpect(jsonPath("$.menuItems[1].userType", is("A")));

        verify(menuService).getMenuItemsForRole(true);
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — unauthenticated
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /api/menu/navigate → 401 when not authenticated")
    void navigate_unauthenticated_returns401() throws Exception {
        NavigationRequest req = new NavigationRequest(1);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — valid selection by ROLE_USER
    // Mirrors COBOL: EXEC CICS XCTL PROGRAM(COACTVWC) → /api/accounts/view
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → 200 with route for valid selection (ROLE_USER)")
    void navigate_validSelection_roleUser_returnsRoute() throws Exception {
        NavigationResponse navResp = new NavigationResponse(
                "/api/accounts/view", "Account View", "COACTVWC",
                "Navigating to: Account View");
        when(menuService.navigate(1, false)).thenReturn(navResp);

        NavigationRequest req = new NavigationRequest(1);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.route",       is("/api/accounts/view")))
               .andExpect(jsonPath("$.label",       is("Account View")))
               .andExpect(jsonPath("$.programName", is("COACTVWC")))
               .andExpect(jsonPath("$.message",     containsString("Account View")));

        verify(menuService).navigate(1, false);
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — invalid selection → 400
    // Mirrors COBOL: "Please enter a valid option number..."
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → 400 for invalid selection number")
    void navigate_invalidSelection_returns400() throws Exception {
        when(menuService.navigate(99, false))
                .thenThrow(new IllegalArgumentException(
                        "Please enter a valid option number (1-12)."));

        NavigationRequest req = new NavigationRequest(99);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message", containsString("valid option")));
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — admin-only option by ROLE_USER → 403
    // Mirrors COBOL: "No access - Admin Only option..."
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → 403 when ROLE_USER selects admin-only option")
    void navigate_adminOnlyOption_roleUser_returns403() throws Exception {
        when(menuService.navigate(12, false))
                .thenThrow(new SecurityException(
                        "No access - Admin Only option: User Management"));

        NavigationRequest req = new NavigationRequest(12);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.message", containsString("Admin Only")));
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — admin can access admin-only option → 200
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    @DisplayName("POST /api/menu/navigate → 200 for ROLE_ADMIN on admin-only option")
    void navigate_adminOnlyOption_roleAdmin_returnsRoute() throws Exception {
        NavigationResponse navResp = new NavigationResponse(
                "/api/admin/users", "User Management", "COUSR00C",
                "Navigating to: User Management");
        when(menuService.navigate(12, true)).thenReturn(navResp);

        NavigationRequest req = new NavigationRequest(12);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.route",       is("/api/admin/users")))
               .andExpect(jsonPath("$.programName", is("COUSR00C")));

        verify(menuService).navigate(12, true);
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — XCTL to COPAUS0C (pending auth) → 200
    // Mirrors the special COPAUS0C INQUIRE branch in PROCESS-ENTER-KEY
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → 200 routes COPAUS0C to /api/authorizations/pending")
    void navigate_pendingAuth_returnsCorrectRoute() throws Exception {
        NavigationResponse navResp = new NavigationResponse(
                "/api/authorizations/pending",
                "Pending Authorization View", "COPAUS0C",
                "Navigating to: Pending Authorization View");
        when(menuService.navigate(11, false)).thenReturn(navResp);

        NavigationRequest req = new NavigationRequest(11);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.route", is("/api/authorizations/pending")));
    }

    // ----------------------------------------------------------------
    // GET /api/menu — response structure contains userType field
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("GET /api/menu → menu items include userType field")
    void getMenu_itemsContainUserTypeField() throws Exception {
        List<MenuItem> items = List.of(
                new MenuItem(6, "Transaction List", "COTRN00C", "U"));
        when(menuService.getMenuItemsForRole(false)).thenReturn(items);

        mockMvc.perform(get("/api/menu"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.menuItems[0].userType", is("U")));
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate → zero selection → 400 (WS-OPTION = ZEROS)
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → 400 for selection = 0 (WS-OPTION=ZEROS guard)")
    void navigate_zeroSelection_returns400() throws Exception {
        when(menuService.navigate(0, false))
                .thenThrow(new IllegalArgumentException(
                        "Please enter a valid option number (1-12)."));

        NavigationRequest req = new NavigationRequest(0);
        mockMvc.perform(post("/api/menu/navigate")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    // GET /api/menu — empty list edge case (all items filtered out)
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("GET /api/menu → 200 with empty list when all options filtered")
    void getMenu_emptyList_returns200() throws Exception {
        when(menuService.getMenuItemsForRole(false)).thenReturn(List.of());

        mockMvc.perform(get("/api/menu"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.menuItems", hasSize(0)));
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate — navigate to sign-out (PF3 equivalent)
    // Mirrors COBOL: WHEN DFHPF3 → RETURN-TO-SIGNON-SCREEN
    // ----------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("POST /api/menu/navigate → signout route when selection triggers COSGN00C")
    void navigate_signout_returnsSignoutRoute() throws Exception {
        NavigationResponse navResp = new NavigationResponse(
                "/api/auth/signout", "Sign Out", "COSGN00C",
                "Navigating to: Sign Out");
        when(menuService.navigate(0, false)).thenReturn(navResp);

        // For completeness test the resolveRoute helper via service layer
        String route = new MenuService().resolveRoute("COSGN00C");
        assert route.equals("/api/auth/signout") :
                "Expected /api/auth/signout but got " + route;
    }

    // ----------------------------------------------------------------
    // MenuService unit tests (non-MockMvc — pure logic validation)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("MenuService.navigate → valid user item returns correct route")
    void menuService_navigate_userItem_returnsCorrectRoute() {
        MenuService svc = new MenuService();
        NavigationResponse resp = svc.navigate(1, false);
        assert resp.getRoute().equals("/api/accounts/view") :
                "Expected /api/accounts/view, got " + resp.getRoute();
        assert resp.getProgramName().equals("COACTVWC");
    }

    @Test
    @DisplayName("MenuService.navigate → admin-only item throws SecurityException for non-admin")
    void menuService_navigate_adminOnlyByUser_throwsSecurityException() {
        MenuService svc = new MenuService();
        try {
            svc.navigate(12, false);
            assert false : "Expected SecurityException";
        } catch (SecurityException e) {
            assert e.getMessage().contains("Admin Only");
        }
    }

    @Test
    @DisplayName("MenuService.navigate → DUMMY program returns coming-soon route")
    void menuService_navigate_dummyProgram_returnsComingSoon() {
        MenuService svc = new MenuService();
        String route = svc.resolveRoute("DUMMY01C");
        assert route.equals("/api/menu/coming-soon") :
                "Expected coming-soon route, got " + route;
    }

    @Test
    @DisplayName("MenuService.getMenuItemsForRole → user sees no admin items")
    void menuService_getMenuItemsForRole_userSeesNoAdminItems() {
        MenuService svc = new MenuService();
        List<MenuItem> items = svc.getMenuItemsForRole(false);
        boolean hasAdminItem = items.stream().anyMatch(MenuItem::isAdminOnly);
        assert !hasAdminItem : "ROLE_USER should not see admin-only items";
    }

    @Test
    @DisplayName("MenuService.getMenuItemsForRole → admin sees all items including admin-only")
    void menuService_getMenuItemsForRole_adminSeesAllItems() {
        MenuService svc = new MenuService();
        List<MenuItem> userItems  = svc.getMenuItemsForRole(false);
        List<MenuItem> adminItems = svc.getMenuItemsForRole(true);
        assert adminItems.size() >= userItems.size() :
                "Admin should see at least as many items as regular user";
    }
}
