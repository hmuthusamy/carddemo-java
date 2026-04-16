package com.carddemo.controller;

import com.carddemo.model.AdminMenuRequest;
import com.carddemo.model.AdminMenuResponse;
import com.carddemo.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminController – REST translation of COADM01C CICS COBOL program.
 *
 * CICS interaction → HTTP mapping
 * ──────────────────────────────────────────────────────────────────────────
 * EXEC CICS SEND MAP (COADM1A)  →  GET  /api/admin/menu
 *                                   responds with AdminMenuResponse (200)
 *
 * EXEC CICS RECEIVE MAP          →  @RequestBody AdminMenuRequest on POST
 *
 * EIBAID = DFHENTER              →  POST /api/admin/menu/select
 *                                   body: { "optionSelected": <n> }
 *
 * EIBAID = DFHPF3 (PF3 key)     →  POST /api/admin/menu/pf3
 *                                   redirects to signon (302)
 *
 * EIBAID = OTHER (invalid key)   →  POST /api/admin/menu/select  with
 *                                   out-of-range option → 400 Bad Request
 *
 * EXEC CICS XCTL PROGRAM(…)     →  302 Location: /api/<target>
 *   COUSR00C → /api/users
 *   COUSR01C → /api/users/add
 *   COUSR02C → /api/users/update
 *   COUSR03C → /api/users/delete
 *   COTRTLIC → /api/transactions/types
 *   COTRTUPC → /api/transactions/maintenance
 *
 * EXEC CICS RETURN TRANSID(CA00) →  HTTP 200 with redirectTo hint
 *
 * Security: all endpoints require ROLE_ADMIN (mirrors CICS admin user check).
 * ──────────────────────────────────────────────────────────────────────────
 * COBOL paragraphs mapped:
 *   MAIN-PARA                   → getMenu() + selectOption()
 *   SEND-MENU-SCREEN            → getMenu()
 *   RECEIVE-MENU-SCREEN         → selectOption() @RequestBody
 *   PROCESS-ENTER-KEY           → selectOption() → adminService.processEnterKey()
 *   RETURN-TO-SIGNON-SCREEN     → pressPf3()
 *   POPULATE-HEADER-INFO        → AdminService.populateHeaderInfo()
 *   BUILD-MENU-OPTIONS          → AdminService.buildMenuOptions()
 *   PGMIDERR-ERR-PARA           → selectOption() not-installed branch
 *
 * Individual option forwarding (XCTL):
 *   getUserList()               → option 1  COUSR00C
 *   addUser()                   → option 2  COUSR01C
 *   updateUser()                → option 3  COUSR02C
 *   deleteUser()                → option 4  COUSR03C
 *   getTransactionTypeList()    → option 5  COTRTLIC
 *   maintainTransactionType()   → option 6  COTRTUPC
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/menu
    //
    // Maps: EXEC CICS SEND MAP ('COADM1A') → initial menu display.
    // Returns the full AdminMenuResponse with header + all 6 options.
    // HTTP 200 OK.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/menu")
    public ResponseEntity<AdminMenuResponse> getMenu() {
        AdminMenuResponse response = adminService.buildMenuScreen();
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/select
    //
    // Maps: EIBAID = DFHENTER → PROCESS-ENTER-KEY.
    // Body: AdminMenuRequest { "optionSelected": 1-6 }
    //
    // Outcomes:
    //   • Invalid option (0 / >6 / null) → 400 Bad Request  + error message
    //   • DUMMY program                  → 200 OK           + not-installed msg
    //   • Valid option                   → 302 Found        + Location header
    //     (mirrors EXEC CICS XCTL PROGRAM(CDEMO-ADMIN-OPT-PGMNAME(WS-OPTION)))
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/menu/select")
    public ResponseEntity<AdminMenuResponse> selectOption(
            @Valid @RequestBody AdminMenuRequest request) {

        // Null / zero option → equivalent to COBOL "WS-OPTION = ZEROS" guard
        if (request.getOptionSelected() == null || request.getOptionSelected() == 0) {
            AdminMenuResponse errResp = adminService.buildMenuScreen();
            errResp.setMessage(AdminService.MSG_INVALID_KEY);
            return ResponseEntity.badRequest().body(errResp);
        }

        AdminMenuResponse response = adminService.processEnterKey(request);

        // Validation failure set a message but no redirectTo
        if (response.getMessage() != null
                && response.getMessage().equals(AdminService.MSG_INVALID_KEY)) {
            return ResponseEntity.badRequest().body(response);
        }

        // Not-installed (PGMIDERR / DUMMY guard)
        if (response.getMessage() != null
                && response.getMessage().equals(AdminService.MSG_NOT_INSTALLED)) {
            return ResponseEntity.ok(response);
        }

        // Success: redirect (EXEC CICS XCTL PROGRAM(…))
        if (response.getRedirectTo() != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", response.getRedirectTo())
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/menu/pf3
    //
    // Maps: EIBAID = DFHPF3 → RETURN-TO-SIGNON-SCREEN.
    // Redirects to signon (COSGN00C → /api/auth/signon).
    // HTTP 302 Found.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/menu/pf3")
    public ResponseEntity<Void> pressPf3() {
        String signonPath = adminService.getSignonRedirect();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", signonPath)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/users
    //
    // Maps: menu option 1 → EXEC CICS XCTL PROGRAM('COUSR00C').
    // User List (Security).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserList() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/users")
                .body(Map.of(
                    "option",  1,
                    "program", "COUSR00C",
                    "message", "Redirecting to User List"
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/users
    //
    // Maps: menu option 2 → EXEC CICS XCTL PROGRAM('COUSR01C').
    // User Add (Security).
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> addUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/users/add")
                .body(Map.of(
                    "option",  2,
                    "program", "COUSR01C",
                    "message", "Redirecting to User Add"
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/admin/users
    //
    // Maps: menu option 3 → EXEC CICS XCTL PROGRAM('COUSR02C').
    // User Update (Security).
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/users")
    public ResponseEntity<Map<String, Object>> updateUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/users/update")
                .body(Map.of(
                    "option",  3,
                    "program", "COUSR02C",
                    "message", "Redirecting to User Update"
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/admin/users
    //
    // Maps: menu option 4 → EXEC CICS XCTL PROGRAM('COUSR03C').
    // User Delete (Security).
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, Object>> deleteUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/users/delete")
                .body(Map.of(
                    "option",  4,
                    "program", "COUSR03C",
                    "message", "Redirecting to User Delete"
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/transactions/types
    //
    // Maps: menu option 5 → EXEC CICS XCTL PROGRAM('COTRTLIC').
    // Transaction Type List/Update (Db2).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/transactions/types")
    public ResponseEntity<Map<String, Object>> getTransactionTypeList() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/transactions/types")
                .body(Map.of(
                    "option",  5,
                    "program", "COTRTLIC",
                    "message", "Redirecting to Transaction Type List"
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/transactions/maintenance
    //
    // Maps: menu option 6 → EXEC CICS XCTL PROGRAM('COTRTUPC').
    // Transaction Type Maintenance (Db2).
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/transactions/maintenance")
    public ResponseEntity<Map<String, Object>> maintainTransactionType() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/api/transactions/maintenance")
                .body(Map.of(
                    "option",  6,
                    "program", "COTRTUPC",
                    "message", "Redirecting to Transaction Type Maintenance"
                ));
    }
}
