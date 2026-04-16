package com.carddemo.controller;

import com.carddemo.model.UserData;
import com.carddemo.service.UserAddService;
import com.carddemo.service.UserAlreadyExistsException;
import com.carddemo.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.carddemo.config.SecurityConfig;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserManagementControllerTest – integration tests for the Spring Boot REST
 * controller migrated from COBOL/CICS COUSR00C and COUSR01C program families.
 *
 * <h2>Test Coverage</h2>
 * Each test maps to the corresponding COBOL function that was migrated:
 * <ul>
 *   <li>COUSR00C STARTBR/READNEXT → GET /api/users (paginated list)
 *   <li>COUSR00C READ RIDFLD      → GET /api/users/{userId}
 *   <li>COUSR01C WRITE (CU01)     → POST /api/users → delegates to {@code UserAddService}
 *   <li>COUSR02C REWRITE (sel 'U')→ PUT  /api/users/{userId}
 *   <li>COUSR03C DELETE  (sel 'D')→ DELETE /api/users/{userId}
 *   <li>Security: 403 for non-ADMIN roles
 * </ul>
 */
@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService userManagementService;

    /** COUSR01C-specific add-user service (required since COUSR01C migration). */
    @MockBean
    private UserAddService userAddService;

    @Autowired
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private UserData adminUser;
    private UserData regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserData("ADMIN001", "Alice", "Admin",
                                 "$2a$10$hashedpwd1", "A");
        regularUser = new UserData("USR00001", "Bob", "Smith",
                                   "$2a$10$hashedpwd2", "U");
    }

    // -----------------------------------------------------------------------
    // GET /api/users – COUSR00C STARTBR/READNEXT 10-record page browse
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users – ADMIN role returns paginated user list (COUSR00C list)")
    void listUsers_asAdmin_returns200WithPage() throws Exception {
        Page<UserData> page = new PageImpl<>(
                List.of(adminUser, regularUser),
                PageRequest.of(0, 10),
                2L);
        when(userManagementService.listUsers(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].userId", is("ADMIN001")))
                .andExpect(jsonPath("$.content[1].userId", is("USR00001")))
                .andExpect(jsonPath("$.totalElements", is(2)));

        verify(userManagementService).listUsers(0, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users – default page params (page=0, size=10)")
    void listUsers_defaultParams_usesDefaults() throws Exception {
        Page<UserData> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L);
        when(userManagementService.listUsers(0, 10)).thenReturn(emptyPage);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/users – non-ADMIN role returns 403 Forbidden")
    void listUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userManagementService);
    }

    @Test
    @DisplayName("GET /api/users – unauthenticated returns 401")
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // GET /api/users/{userId} – EXEC CICS READ RIDFLD(SEC-USR-ID)
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{userId} – existing user returns 200")
    void getUserById_found_returns200() throws Exception {
        when(userManagementService.getUserById("ADMIN001"))
                .thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/api/users/ADMIN001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId",    is("ADMIN001")))
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.lastName",  is("Admin")))
                .andExpect(jsonPath("$.userType",  is("A")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{userId} – missing user returns 404 (COBOL NOTFND)")
    void getUserById_notFound_returns404() throws Exception {
        when(userManagementService.getUserById("MISSING1"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/MISSING1"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/users – COUSR01C EXEC CICS WRITE (transaction CU01)
    // Delegates to UserAddService (COUSR01C-specific service)
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users – valid request creates user, returns 201 (COUSR01C WRITE NORMAL)")
    void createUser_validRequest_returns201() throws Exception {
        // Mirrors COUSR01C PROCESS-ENTER-KEY valid input
        UserManagementController.AddUserRequest req =
                new UserManagementController.AddUserRequest(
                        "NEWUSR01", "Carol", "Jones", "Secret1!", "R");

        UserData saved = new UserData("NEWUSR01", "Carol", "Jones",
                                      "$2a$10$encoded", "R");
        when(userAddService.addUser("NEWUSR01", "Carol", "Jones", "Secret1!", "R"))
                .thenReturn(saved);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId",   is("NEWUSR01")))
                .andExpect(jsonPath("$.lastName", is("Jones")))
                .andExpect(jsonPath("$.message",  containsString("has been added")));

        verify(userAddService).addUser("NEWUSR01", "Carol", "Jones", "Secret1!", "R");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users – duplicate userId returns 409 (COUSR01C DFHRESP DUPKEY/DUPREC)")
    void createUser_duplicateId_returns409() throws Exception {
        // COBOL: WHEN DFHRESP(DUPKEY) / WHEN DFHRESP(DUPREC)
        //   MOVE 'User ID already exist...' TO WS-MESSAGE
        UserManagementController.AddUserRequest req =
                new UserManagementController.AddUserRequest(
                        "ADMIN001", "Dup", "User", "pass", "A");

        when(userAddService.addUser(eq("ADMIN001"), anyString(), anyString(),
                                    anyString(), anyString()))
                .thenThrow(new UserAlreadyExistsException("ADMIN001"));

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_USER")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users – blank userId returns 400 (COUSR01C: User ID can NOT be empty...)")
    void createUser_blankUserId_returns400() throws Exception {
        // COBOL: WHEN USERIDI = SPACES OR LOW-VALUES
        //   MOVE 'User ID can NOT be empty...' TO WS-MESSAGE
        String badJson = "{\"userId\":\"\",\"firstName\":\"A\",\"lastName\":\"B\","
                       + "\"password\":\"pw\",\"userType\":\"R\"}";

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users – missing required fields returns 400")
    void createUser_missingFields_returns400() throws Exception {
        // userId is blank – violates @NotBlank
        String badJson = "{\"userId\":\"\",\"password\":\"pw\",\"userType\":\"R\"}";

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users – invalid userType returns 400 (COUSR01C role validation)")
    void createUser_invalidUserType_returns400() throws Exception {
        String badJson = "{\"userId\":\"USR00001\",\"firstName\":\"A\",\"lastName\":\"B\","
                       + "\"password\":\"pw\",\"userType\":\"X\"}";

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // PUT /api/users/{userId} – COUSR02C EXEC CICS REWRITE (sel flag 'U')
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/users/{userId} – valid update returns 200 (COUSR02C REWRITE)")
    void updateUser_validRequest_returns200() throws Exception {
        UserData patch = new UserData();
        patch.setFirstName("Updated");
        patch.setLastName("Name");
        patch.setUserType("A");

        UserData updated = new UserData("USR00001", "Updated", "Name",
                                        "$2a$10$hashedpwd2", "A");
        when(userManagementService.updateUser(eq("USR00001"), any(UserData.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/USR00001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.userType",  is("A")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/users/{userId} – non-existent user returns 404")
    void updateUser_notFound_returns404() throws Exception {
        when(userManagementService.updateUser(eq("GHOST001"), any(UserData.class)))
                .thenThrow(new NoSuchElementException("User not found: GHOST001"));

        mockMvc.perform(put("/api/users/GHOST001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserData())))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/users/{userId} – COUSR03C EXEC CICS DELETE (sel flag 'D')
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/users/{userId} – deactivates user, returns 204 (COUSR03C DELETE)")
    void deactivateUser_existing_returns204() throws Exception {
        doNothing().when(userManagementService).deactivateUser("USR00001");

        mockMvc.perform(delete("/api/users/USR00001")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userManagementService).deactivateUser("USR00001");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/users/{userId} – non-existent user returns 404")
    void deactivateUser_notFound_returns404() throws Exception {
        doThrow(new NoSuchElementException("User not found: GHOST001"))
                .when(userManagementService).deactivateUser("GHOST001");

        mockMvc.perform(delete("/api/users/GHOST001")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/users/{userId} – non-ADMIN role returns 403")
    void deactivateUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/USR00001")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userManagementService);
    }
}
