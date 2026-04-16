package com.carddemo.controller;

import com.carddemo.model.AdminMenuRequest;
import com.carddemo.model.AdminMenuResponse;
import com.carddemo.model.AdminMenuResponse.AdminMenuOption;
import com.carddemo.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminControllerTest – @WebMvcTest covering all REST endpoints in AdminController.
 *
 * Each test documents its COBOL origin paragraph and verifies the HTTP contract.
 *
 * Test coverage
 * ─────────────────────────────────────────────────────────────────────────
 *  1. GET  /api/admin/menu              → 200  SEND-MENU-SCREEN (initial)
 *  2. GET  /api/admin/menu (unauth)     → 401/302 security guard
 *  3. POST /api/admin/menu/select opt=1 → 302  COUSR00C redirect
 *  4. POST /api/admin/menu/select opt=2 → 302  COUSR01C redirect
 *  5. POST /api/admin/menu/select opt=3 → 302  COUSR02C redirect
 *  6. POST /api/admin/menu/select opt=4 → 302  COUSR03C redirect
 *  7. POST /api/admin/menu/select opt=5 → 302  COTRTLIC redirect
 *  8. POST /api/admin/menu/select opt=6 → 302  COTRTUPC redirect
 *  9. POST /api/admin/menu/select opt=0 → 400  WS-OPTION=ZEROS guard
 * 10. POST /api/admin/menu/select opt=9 → 400  out-of-range guard
 * 11. POST /api/admin/menu/select DUMMY → 200  PGMIDERR-ERR-PARA
 * 12. POST /api/admin/menu/pf3          → 302  RETURN-TO-SIGNON-SCREEN
 * 13. GET  /api/admin/users             → 302  option 1 shortcut
 * 14. POST /api/admin/users             → 302  option 2 shortcut
 * 15. GET  /api/admin/transactions/types           → 302  option 5 shortcut
 * 16. POST /api/admin/transactions/maintenance     → 302  option 6 shortcut
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminMenuResponse sampleMenuResponse;

    @BeforeEach
    void setUp() {
        sampleMenuResponse = new AdminMenuResponse();
        sampleMenuResponse.setTitle1("AWS CardDemo");
        sampleMenuResponse.setTitle2("Admin Menu");
        sampleMenuResponse.setTransactionId("CA00");
        sampleMenuResponse.setProgramName("COADM01C");
        sampleMenuResponse.setCurrentDate(LocalDate.of(2024, 1, 21));
        sampleMenuResponse.setCurrentTime(LocalTime.of(17, 49, 0));
        sampleMenuResponse.setMenuOptions(List.of(
            new AdminMenuOption(1, "User List (Security)",               "COUSR00C", "/api/users"),
            new AdminMenuOption(2, "User Add (Security)",                "COUSR01C", "/api/users/add"),
            new AdminMenuOption(3, "User Update (Security)",             "COUSR02C", "/api/users/update"),
            new AdminMenuOption(4, "User Delete (Security)",             "COUSR03C", "/api/users/delete"),
            new AdminMenuOption(5, "Transaction Type List/Update (Db2)", "COTRTLIC", "/api/transactions/types"),
            new AdminMenuOption(6, "Transaction Type Maintenance (Db2)", "COTRTUPC", "/api/transactions/maintenance")
        ));
        sampleMenuResponse.setMessage("");
    }

    // ── Test 1 ────────────────────────────────────────────────────────────────
    // GET /api/admin/menu → 200 + AdminMenuResponse body
    // Maps: EXEC CICS SEND MAP ('COADM1A') initial display
    @Test
    @DisplayName("GET /api/admin/menu returns 200 with full menu (SEND-MENU-SCREEN)")
    @WithMockUser(roles = "ADMIN")
    void getMenu_returnsOkWithMenuResponse() throws Exception {
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        mockMvc.perform(get("/api/admin/menu"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title1").value("AWS CardDemo"))
                .andExpect(jsonPath("$.title2").value("Admin Menu"))
                .andExpect(jsonPath("$.transactionId").value("CA00"))
                .andExpect(jsonPath("$.programName").value("COADM01C"))
                .andExpect(jsonPath("$.menuOptions").isArray())
                .andExpect(jsonPath("$.menuOptions.length()").value(6))
                .andExpect(jsonPath("$.menuOptions[0].name").value("User List (Security)"))
                .andExpect(jsonPath("$.menuOptions[5].name").value("Transaction Type Maintenance (Db2)"));
    }

    // ── Test 2 ────────────────────────────────────────────────────────────────
    // Unauthenticated GET → 302/401 (Spring Security blocks non-ADMIN)
    @Test
    @DisplayName("GET /api/admin/menu without authentication returns 401/302")
    void getMenu_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/menu"))
                .andDo(print())
                .andExpect(status().is(org.hamcrest.Matchers.either(
                        org.hamcrest.Matchers.is(401))
                        .or(org.hamcrest.Matchers.is(302))));
    }

    // ── Test 3 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=1 → 302 redirect to /api/users
    // Maps: PROCESS-ENTER-KEY → EXEC CICS XCTL PROGRAM('COUSR00C')
    @Test
    @DisplayName("POST /api/admin/menu/select option=1 redirects to /api/users (COUSR00C)")
    @WithMockUser(roles = "ADMIN")
    void selectOption1_redirectsToUserList() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(1);
        redirectResp.setRedirectTo("/api/users");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(1);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users"));
    }

    // ── Test 4 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=2 → 302 redirect to /api/users/add
    // Maps: PROCESS-ENTER-KEY → EXEC CICS XCTL PROGRAM('COUSR01C')
    @Test
    @DisplayName("POST /api/admin/menu/select option=2 redirects to /api/users/add (COUSR01C)")
    @WithMockUser(roles = "ADMIN")
    void selectOption2_redirectsToUserAdd() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(2);
        redirectResp.setRedirectTo("/api/users/add");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(2);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users/add"));
    }

    // ── Test 5 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=3 → 302 redirect to /api/users/update
    @Test
    @DisplayName("POST /api/admin/menu/select option=3 redirects to /api/users/update (COUSR02C)")
    @WithMockUser(roles = "ADMIN")
    void selectOption3_redirectsToUserUpdate() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(3);
        redirectResp.setRedirectTo("/api/users/update");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(3);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users/update"));
    }

    // ── Test 6 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=4 → 302 redirect to /api/users/delete
    @Test
    @DisplayName("POST /api/admin/menu/select option=4 redirects to /api/users/delete (COUSR03C)")
    @WithMockUser(roles = "ADMIN")
    void selectOption4_redirectsToUserDelete() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(4);
        redirectResp.setRedirectTo("/api/users/delete");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(4);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users/delete"));
    }

    // ── Test 7 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=5 → 302 redirect to /api/transactions/types
    @Test
    @DisplayName("POST /api/admin/menu/select option=5 redirects to /api/transactions/types (COTRTLIC)")
    @WithMockUser(roles = "ADMIN")
    void selectOption5_redirectsToTransactionTypeList() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(5);
        redirectResp.setRedirectTo("/api/transactions/types");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(5);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/transactions/types"));
    }

    // ── Test 8 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=6 → 302 redirect to /api/transactions/maintenance
    @Test
    @DisplayName("POST /api/admin/menu/select option=6 redirects to /api/transactions/maintenance (COTRTUPC)")
    @WithMockUser(roles = "ADMIN")
    void selectOption6_redirectsToTransactionMaintenance() throws Exception {
        AdminMenuResponse redirectResp = new AdminMenuResponse();
        redirectResp.setSelectedOption(6);
        redirectResp.setRedirectTo("/api/transactions/maintenance");
        redirectResp.setMessage("");
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(redirectResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(6);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/transactions/maintenance"));
    }

    // ── Test 9 ────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=0 → 400 (WS-OPTION = ZEROS guard)
    @Test
    @DisplayName("POST /api/admin/menu/select option=0 returns 400 (WS-OPTION=ZEROS guard)")
    @WithMockUser(roles = "ADMIN")
    void selectOption0_returnsBadRequest() throws Exception {
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(0);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ── Test 10 ───────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select option=9 → 400 (>CDEMO-ADMIN-OPT-COUNT guard)
    @Test
    @DisplayName("POST /api/admin/menu/select option=9 returns 400 (out-of-range guard)")
    @WithMockUser(roles = "ADMIN")
    void selectOptionOutOfRange_returnsBadRequest() throws Exception {
        AdminMenuResponse errResp = new AdminMenuResponse();
        errResp.setSelectedOption(9);
        errResp.setMessage(AdminService.MSG_INVALID_KEY);
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(errResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(9);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ── Test 11 ───────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select DUMMY option → 200 + not-installed message
    // Maps: PGMIDERR-ERR-PARA
    @Test
    @DisplayName("POST /api/admin/menu/select DUMMY program returns 200 with not-installed message (PGMIDERR-ERR-PARA)")
    @WithMockUser(roles = "ADMIN")
    void selectDummyOption_returns200WithNotInstalledMessage() throws Exception {
        AdminMenuResponse dummyResp = new AdminMenuResponse();
        dummyResp.setSelectedOption(3);
        dummyResp.setMessage(AdminService.MSG_NOT_INSTALLED);
        when(adminService.processEnterKey(any(AdminMenuRequest.class))).thenReturn(dummyResp);
        when(adminService.buildMenuScreen()).thenReturn(sampleMenuResponse);

        AdminMenuRequest req = new AdminMenuRequest(3);
        mockMvc.perform(post("/api/admin/menu/select")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AdminService.MSG_NOT_INSTALLED));
    }

    // ── Test 12 ───────────────────────────────────────────────────────────────
    // POST /api/admin/menu/pf3 → 302 to /api/auth/signon
    // Maps: EIBAID = DFHPF3 → RETURN-TO-SIGNON-SCREEN
    @Test
    @DisplayName("POST /api/admin/menu/pf3 redirects to signon (RETURN-TO-SIGNON-SCREEN)")
    @WithMockUser(roles = "ADMIN")
    void pressPf3_redirectsToSignon() throws Exception {
        when(adminService.getSignonRedirect()).thenReturn("/api/auth/signon");

        mockMvc.perform(post("/api/admin/menu/pf3")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/auth/signon"));
    }

    // ── Test 13 ───────────────────────────────────────────────────────────────
    // GET /api/admin/users → 302 to /api/users (option 1 shortcut)
    @Test
    @DisplayName("GET /api/admin/users returns 302 redirect to /api/users (option 1 COUSR00C)")
    @WithMockUser(roles = "ADMIN")
    void getUserList_redirectsToApiUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users"));
    }

    // ── Test 14 ───────────────────────────────────────────────────────────────
    // POST /api/admin/users → 302 to /api/users/add (option 2 shortcut)
    @Test
    @DisplayName("POST /api/admin/users returns 302 redirect to /api/users/add (option 2 COUSR01C)")
    @WithMockUser(roles = "ADMIN")
    void addUser_redirectsToApiUsersAdd() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/users/add"));
    }

    // ── Test 15 ───────────────────────────────────────────────────────────────
    // GET /api/admin/transactions/types → 302 (option 5 shortcut)
    @Test
    @DisplayName("GET /api/admin/transactions/types returns 302 redirect to /api/transactions/types (COTRTLIC)")
    @WithMockUser(roles = "ADMIN")
    void getTransactionTypeList_redirects() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/types"))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/transactions/types"));
    }

    // ── Test 16 ───────────────────────────────────────────────────────────────
    // POST /api/admin/transactions/maintenance → 302 (option 6 shortcut)
    @Test
    @DisplayName("POST /api/admin/transactions/maintenance returns 302 redirect (COTRTUPC)")
    @WithMockUser(roles = "ADMIN")
    void maintainTransactionType_redirects() throws Exception {
        mockMvc.perform(post("/api/admin/transactions/maintenance")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/transactions/maintenance"));
    }
}
