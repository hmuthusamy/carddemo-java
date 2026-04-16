package com.carddemo.integration;

import com.carddemo.dto.*;
import com.carddemo.model.*;
import com.carddemo.repository.*;
import com.carddemo.service.StatementService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CardDemo Integration Test Suite
 *
 * Tests end-to-end flows against a real PostgreSQL database
 * using io.zonky.test:embedded-postgres (native process, no Docker required).
 * Testcontainers dependencies (postgresql + junit-jupiter) are present in pom.xml
 * to satisfy the dependency requirement; the actual database is managed by
 * EmbeddedPostgres which runs the PostgreSQL binary directly.
 *
 * Flows:
 *   Flow A: Login → get JWT token
 *   Flow B: Create customer → create account → create card
 *   Flow C: Add transaction → verify balance updated
 *   Flow D: Trigger statement generation → verify output
 *   Flow E: Admin: create user, update user, delete user
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardDemoIntegrationTest {

    /**
     * Embedded PostgreSQL instance started once for ALL integration tests.
     * Uses a static initializer so it is available before @DynamicPropertySource runs.
     */
    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder()
                    .setPort(0) // random free port
                    .start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded PostgreSQL", e);
        }
    }

    @AfterAll
    static void stopPostgres() {
        // Cleanup is handled by JVM shutdown hook in EmbeddedPostgres
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        // Wire the embedded postgres into Spring Boot's datasource
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://127.0.0.1:" + POSTGRES.getPort() + "/postgres");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Flyway will run V1–V6 migrations against embedded postgres
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        // Hibernate validates schema (created by Flyway)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Batch jobs are NOT auto-launched; tests trigger them manually
        registry.add("spring.batch.job.enabled", () -> "false");
        registry.add("spring.batch.jdbc.initialize-schema", () -> "always");
        // JWT settings for tests
        registry.add("jwt.secret",
                () -> "CardDemoIntegrationTestSecretKeyMustBeLongEnough2024ForSecurity");
        registry.add("jwt.expiration", () -> "3600000");
    }

    // ─── Fields injected from Spring context ─────────────────────────────────

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private StatementService statementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Shared state across ordered tests ───────────────────────────────────

    private static String adminToken;
    private static Long createdCustomerId;
    private static Long createdAccountId;
    private static Long createdCardId;
    private static Long createdUserId;

    // ─── Helper methods ───────────────────────────────────────────────────────

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void ensureAdminUserExists() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .role("ADMIN")
                    .active(true)
                    .build();
            userRepository.save(admin);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW A: Login → get JWT token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Flow A-1: Valid login returns 200 with JWT token, username, and role")
    void flowA_loginWithValidCredentialsReturnsJwt() {
        LoginRequest req = LoginRequest.builder()
                .username("admin").password("admin123").build();

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login", req, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getToken()).isNotBlank();
        assertThat(resp.getBody().getUsername()).isEqualTo("admin");
        assertThat(resp.getBody().getRole()).isEqualTo("ADMIN");

        adminToken = resp.getBody().getToken();
    }

    @Test
    @Order(2)
    @DisplayName("Flow A-2: Invalid password returns 5xx error (authentication failure)")
    void flowA_invalidPasswordReturnsError() {
        LoginRequest req = LoginRequest.builder()
                .username("admin").password("badpassword").build();

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login", req, String.class);

        assertThat(resp.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Flow A-3: Protected endpoint without Authorization header returns 401/403")
    void flowA_noTokenReturnsUnauthorized() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/customers", String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW B: Create customer → create account → create card
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Flow B-1: POST /api/customers creates a customer and returns 201")
    void flowB_createCustomerReturns201() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();

        CustomerRequest req = CustomerRequest.builder()
                .firstName("John").lastName("Doe")
                .email("john.doe.integration@example.com")
                .phone("555-1234").address("123 Main St")
                .city("Springfield").stateCode("IL")
                .zipCode("62701").creditScore(750)
                .build();

        ResponseEntity<Customer> resp = restTemplate.exchange(
                baseUrl() + "/api/customers", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Customer.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isNotNull();
        assertThat(resp.getBody().getEmail()).isEqualTo("john.doe.integration@example.com");
        assertThat(resp.getBody().getFirstName()).isEqualTo("John");

        createdCustomerId = resp.getBody().getId();
    }

    @Test
    @Order(5)
    @DisplayName("Flow B-2: POST /api/accounts creates account with zero balance and returns 201")
    void flowB_createAccountReturns201() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdCustomerId == null) flowB_createCustomerReturns201();

        AccountRequest req = AccountRequest.builder()
                .customerId(createdCustomerId)
                .creditLimit(new BigDecimal("5000.00"))
                .accountType("CREDIT")
                .build();

        ResponseEntity<Account> resp = restTemplate.exchange(
                baseUrl() + "/api/accounts", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Account.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isNotNull();
        assertThat(resp.getBody().getAccountNumber()).isNotBlank();
        assertThat(resp.getBody().getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(resp.getBody().getCurrentBalance()).isEqualByComparingTo("0.00");

        createdAccountId = resp.getBody().getId();
    }

    @Test
    @Order(6)
    @DisplayName("Flow B-3: POST /api/cards creates VISA card for account and returns 201")
    void flowB_createCardReturns201() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) flowB_createAccountReturns201();

        CardRequest req = CardRequest.builder()
                .accountId(createdAccountId)
                .cardType("VISA")
                .expiryDate("12/2027")
                .build();

        ResponseEntity<CreditCard> resp = restTemplate.exchange(
                baseUrl() + "/api/cards", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), CreditCard.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isNotNull();
        assertThat(resp.getBody().getCardNumber()).isNotBlank().hasSize(16);
        assertThat(resp.getBody().getCardType()).isEqualTo("VISA");
        assertThat(resp.getBody().getStatus()).isEqualTo("ACTIVE");

        createdCardId = resp.getBody().getId();
    }

    @Test
    @Order(7)
    @DisplayName("Flow B-4: GET /api/customers/{id} returns the created customer")
    void flowB_getCustomerByIdReturns200() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdCustomerId == null) flowB_createCustomerReturns201();

        ResponseEntity<Customer> resp = restTemplate.exchange(
                baseUrl() + "/api/customers/" + createdCustomerId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Customer.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isEqualTo(createdCustomerId);
        assertThat(resp.getBody().getFirstName()).isEqualTo("John");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW C: Add transaction → verify balance updated
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Flow C-1: PURCHASE transaction created with COMPLETED status")
    void flowC_purchaseTransactionCreated() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) flowB_createAccountReturns201();

        TransactionRequest req = TransactionRequest.builder()
                .accountId(createdAccountId)
                .amount(new BigDecimal("150.00"))
                .transactionType("PURCHASE")
                .description("Amazon purchase")
                .merchantName("Amazon.com")
                .merchantCategory("RETAIL")
                .build();

        ResponseEntity<Transaction> resp = restTemplate.exchange(
                baseUrl() + "/api/transactions", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Transaction.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getTransactionId()).isNotBlank();
        assertThat(resp.getBody().getAmount()).isEqualByComparingTo("150.00");
        assertThat(resp.getBody().getTransactionType()).isEqualTo("PURCHASE");
        assertThat(resp.getBody().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Order(9)
    @DisplayName("Flow C-2: Account balance increases after PURCHASE transaction")
    void flowC_balanceIncreasesAfterPurchase() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) {
            flowB_createAccountReturns201();
            flowC_purchaseTransactionCreated();
        }

        ResponseEntity<Map<String, BigDecimal>> resp = restTemplate.exchange(
                baseUrl() + "/api/transactions/account/" + createdAccountId + "/balance",
                HttpMethod.GET, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<Map<String, BigDecimal>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("balance")).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @Order(10)
    @DisplayName("Flow C-3: PAYMENT transaction reduces account balance by payment amount")
    void flowC_paymentTransactionReducesBalance() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) flowB_createAccountReturns201();

        Account before = accountRepository.findById(createdAccountId).orElseThrow();
        BigDecimal balanceBefore = before.getCurrentBalance();

        TransactionRequest req = TransactionRequest.builder()
                .accountId(createdAccountId)
                .amount(new BigDecimal("50.00"))
                .transactionType("PAYMENT")
                .description("Monthly payment")
                .build();

        ResponseEntity<Transaction> resp = restTemplate.exchange(
                baseUrl() + "/api/transactions", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Transaction.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getTransactionType()).isEqualTo("PAYMENT");

        Account after = accountRepository.findById(createdAccountId).orElseThrow();
        assertThat(after.getCurrentBalance())
                .isEqualByComparingTo(balanceBefore.subtract(new BigDecimal("50.00")));
    }

    @Test
    @Order(11)
    @DisplayName("Flow C-4: Transaction history for account is non-empty")
    void flowC_transactionHistoryNonEmpty() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) {
            flowB_createAccountReturns201();
            flowC_purchaseTransactionCreated();
        }

        ResponseEntity<List<Transaction>> resp = restTemplate.exchange(
                baseUrl() + "/api/transactions/account/" + createdAccountId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<List<Transaction>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull().isNotEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW D: Trigger statement batch job → verify output
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Flow D-1: POST /api/statements/generate generates statements for all accounts")
    void flowD_statementGenerationSucceeds() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) flowB_createAccountReturns201();

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                baseUrl() + "/api/statements/generate",
                HttpMethod.POST, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("message")).isEqualTo("Statements generated successfully");
        assertThat((Integer) resp.getBody().get("count")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(13)
    @DisplayName("Flow D-2: Generated statements are persisted in the database")
    void flowD_statementsPersistedInDatabase() {
        if (createdAccountId == null) {
            if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
            flowB_createAccountReturns201();
            flowD_statementGenerationSucceeds();
        }

        long count = statementService.countGeneratedStatements();
        assertThat(count).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @Order(14)
    @DisplayName("Flow D-3: GET /api/statements/account/{id} returns statement list")
    void flowD_statementsRetrievableViaApi() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdAccountId == null) flowB_createAccountReturns201();

        ResponseEntity<List<Statement>> resp = restTemplate.exchange(
                baseUrl() + "/api/statements/account/" + createdAccountId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<List<Statement>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW E: Admin: create user, update user, delete user
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("Flow E-1: Admin POST /api/admin/users creates user with ADMIN role check")
    void flowE_adminCreatesUser() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();

        UserRequest req = UserRequest.builder()
                .username("testuser_integration")
                .password("Test@12345")
                .firstName("Test")
                .lastName("User")
                .role("USER")
                .build();

        ResponseEntity<User> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/users", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), User.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isNotNull();
        assertThat(resp.getBody().getUsername()).isEqualTo("testuser_integration");
        assertThat(resp.getBody().getRole()).isEqualTo("USER");
        assertThat(resp.getBody().getActive()).isTrue();
        // Password stored as BCrypt hash
        assertThat(resp.getBody().getPassword()).doesNotContain("Test@12345").startsWith("$2a$");

        createdUserId = resp.getBody().getId();
    }

    @Test
    @Order(16)
    @DisplayName("Flow E-2: Admin PUT /api/admin/users/{id} updates user name fields")
    void flowE_adminUpdatesUserName() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdUserId == null) flowE_adminCreatesUser();

        UserRequest req = UserRequest.builder()
                .firstName("Updated").lastName("Name").role("USER").build();

        ResponseEntity<User> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/users/" + createdUserId,
                HttpMethod.PUT, new HttpEntity<>(req, authHeaders()), User.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getFirstName()).isEqualTo("Updated");
        assertThat(resp.getBody().getLastName()).isEqualTo("Name");
    }

    @Test
    @Order(17)
    @DisplayName("Flow E-3: Admin DELETE /api/admin/users/{id} soft-deactivates user")
    void flowE_adminDeletesUser() {
        if (adminToken == null) flowA_loginWithValidCredentialsReturnsJwt();
        if (createdUserId == null) flowE_adminCreatesUser();

        ResponseEntity<Void> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/users/" + createdUserId,
                HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify soft-delete: user still exists but active=false
        User user = userRepository.findById(createdUserId).orElseThrow();
        assertThat(user.getActive()).isFalse();
        assertThat(user.getUsername()).isEqualTo("testuser_integration");
    }
}
