package com.carddemo.controller;

import com.carddemo.model.MenuResponse;
import com.carddemo.model.NavigationRequest;
import com.carddemo.model.NavigationResponse;
import com.carddemo.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * MenuController — REST controller migrated from COMEN01C.CBL.
 *
 * <h2>COBOL → REST mapping</h2>
 * <pre>
 * COBOL (COMEN01C)                     Java REST
 * ─────────────────────────────────    ─────────────────────────────────────
 * SEND-MENU-SCREEN                  →  GET  /api/menu
 * PROCESS-ENTER-KEY                 →  POST /api/menu/navigate
 * EXEC CICS XCTL PROGRAM(...)       →  NavigationResponse.getRoute()
 * RETURN-TO-SIGNON-SCREEN           →  POST /api/menu/navigate → /api/auth/signout
 * CDEMO-USRTYP-USER guard           →  Spring Security ROLE_USER / ROLE_ADMIN
 * </pre>
 *
 * <h2>CICS removal notes</h2>
 * <ul>
 *   <li>{@code EXEC CICS SEND MAP} is replaced by returning JSON.</li>
 *   <li>{@code EXEC CICS RECEIVE MAP} is replaced by {@code @RequestBody}.</li>
 *   <li>{@code EXEC CICS XCTL} is replaced by returning a route string.</li>
 *   <li>COMMAREA state is replaced by stateless JWT/session in SecurityConfig.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ----------------------------------------------------------------
    // GET /api/menu
    // Replaces: SEND-MENU-SCREEN + BUILD-MENU-OPTIONS (COMEN01C)
    // Returns menu items filtered by the authenticated user's role.
    // ----------------------------------------------------------------

    /**
     * Returns the list of menu items available to the requesting user.
     *
     * <p>Equivalent to the COBOL {@code SEND-MENU-SCREEN} paragraph combined
     * with {@code BUILD-MENU-OPTIONS}: iterates CDEMO-MENU-OPTIONS-DATA,
     * suppresses admin-only entries for regular users.
     *
     * @param auth injected by Spring Security; represents current principal
     * @return 200 with {@link MenuResponse}; 401 if unauthenticated
     */
    @GetMapping
    public ResponseEntity<MenuResponse> getMenu(Authentication auth) {
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        MenuResponse response = new MenuResponse(
                menuService.getMenuItemsForRole(isAdmin),
                auth.getName());
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------------------------------
    // POST /api/menu/navigate
    // Replaces: PROCESS-ENTER-KEY → EXEC CICS XCTL PROGRAM(...)
    // ----------------------------------------------------------------

    /**
     * Processes the user's menu selection and returns the target route.
     *
     * <p>Replaces the COBOL {@code PROCESS-ENTER-KEY} paragraph:
     * <ol>
     *   <li>Validates the option number (was {@code WS-OPTION}).</li>
     *   <li>Enforces ROLE_ADMIN guard for admin-only options.</li>
     *   <li>Returns a {@link NavigationResponse} whose {@code route} field
     *       is the REST equivalent of {@code EXEC CICS XCTL PROGRAM(pgm)}.</li>
     * </ol>
     *
     * @param request   body containing {@code selection} (1-based option number)
     * @param auth      current authenticated principal
     * @return 200 with {@link NavigationResponse}; 400 on bad selection;
     *         403 on admin-only access by regular user
     */
    @PostMapping("/navigate")
    public ResponseEntity<?> navigate(@RequestBody NavigationRequest request,
                                      Authentication auth) {
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        try {
            NavigationResponse navResponse =
                    menuService.navigate(request.getSelection(), isAdmin);
            return ResponseEntity.ok(navResponse);

        } catch (IllegalArgumentException ex) {
            // Mirrors COBOL: "Please enter a valid option number..."
            return ResponseEntity.badRequest()
                    .body(new ErrorBody(ex.getMessage()));

        } catch (SecurityException ex) {
            // Mirrors COBOL: "No access - Admin Only option..."
            return ResponseEntity.status(403)
                    .body(new ErrorBody(ex.getMessage()));

        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403)
                    .body(new ErrorBody("Access denied."));
        }
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    /** Minimal error envelope returned on 4xx responses. */
    public record ErrorBody(String message) {}
}
