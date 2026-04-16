package com.carddemo.controller;

import com.carddemo.model.CustomerData;
import com.carddemo.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link CustomerController}.
 *
 * <p>Covers every endpoint migrated from COBOL/CICS COUCUS00:
 * <ul>
 *   <li>GET /api/customers/{customerId}  – COUCUSIN inquiry</li>
 *   <li>GET /api/customers?search={term} – COUCUS00 browse/search</li>
 *   <li>POST /api/customers              – COUCUS00 add customer</li>
 *   <li>PUT  /api/customers/{customerId} – COUCUSUPD update</li>
 * </ul>
 */
@WebMvcTest(CustomerController.class)
@WithMockUser
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerData sampleCustomer;

    @BeforeEach
    void setUp() {
        // Matches CVCUS01Y CUSTOMER-RECORD layout
        sampleCustomer = CustomerData.builder()
                .customerId(100000001L)
                .firstName("John")
                .middleName("A")
                .lastName("Doe")
                .addrLine1("123 Main St")
                .addrLine2("Apt 4B")
                .addrLine3("")
                .addrStateCd("TX")
                .addrCountryCd("USA")
                .addrZip("78701")
                .phoneNum1("512-555-0100")
                .phoneNum2("512-555-0101")
                .ssn(123456789L)
                .govtIssuedId("DL-TX-12345")
                .dateOfBirth("1985-06-15")
                .eftAccountId("EFT0001234")
                .primaryCardHolderInd("Y")
                .ficoCreditScore(720)
                .build();
    }

    // =========================================================================
    // GET /api/customers/{customerId} – COUCUSIN
    // =========================================================================

    @Nested
    @DisplayName("GET /api/customers/{customerId} – COUCUSIN customer inquiry")
    class GetCustomerByIdTests {

        @Test
        @DisplayName("should return 200 OK with customer when found")
        void shouldReturnCustomerWhenFound() throws Exception {
            given(customerService.findById(100000001L))
                    .willReturn(Optional.of(sampleCustomer));

            mockMvc.perform(get("/api/customers/100000001"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId", is(100000001)))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.addrStateCd", is("TX")))
                    .andExpect(jsonPath("$.ficoCreditScore", is(720)));

            verify(customerService).findById(100000001L);
        }

        @Test
        @DisplayName("should return 404 Not Found when customer does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            given(customerService.findById(999999999L))
                    .willReturn(Optional.empty());

            mockMvc.perform(get("/api/customers/999999999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(customerService).findById(999999999L);
        }
    }

    // =========================================================================
    // GET /api/customers?search={term} – COUCUS00 browse/search
    // =========================================================================

    @Nested
    @DisplayName("GET /api/customers – COUCUS00 customer search/browse")
    class SearchCustomersTests {

        @Test
        @DisplayName("should return all customers when no search term provided")
        void shouldReturnAllWhenNoSearchTerm() throws Exception {
            CustomerData customer2 = CustomerData.builder()
                    .customerId(100000002L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            given(customerService.findAll()).willReturn(List.of(sampleCustomer, customer2));

            mockMvc.perform(get("/api/customers"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].customerId", is(100000001)))
                    .andExpect(jsonPath("$[1].customerId", is(100000002)));

            verify(customerService).findAll();
        }

        @Test
        @DisplayName("should search customers by term")
        void shouldSearchCustomersByTerm() throws Exception {
            given(customerService.searchCustomers("Doe"))
                    .willReturn(List.of(sampleCustomer));

            mockMvc.perform(get("/api/customers")
                            .param("search", "Doe"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].lastName", is("Doe")));

            verify(customerService).searchCustomers("Doe");
        }

        @Test
        @DisplayName("should return empty list when no customers match search")
        void shouldReturnEmptyListWhenNoMatches() throws Exception {
            given(customerService.searchCustomers("ZZZNOMATCH"))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/customers")
                            .param("search", "ZZZNOMATCH"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should treat blank search term as 'list all'")
        void shouldTreatBlankSearchAsListAll() throws Exception {
            given(customerService.findAll()).willReturn(List.of(sampleCustomer));

            mockMvc.perform(get("/api/customers")
                            .param("search", "  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(customerService).findAll();
        }
    }

    // =========================================================================
    // POST /api/customers – COUCUS00 create
    // =========================================================================

    @Nested
    @DisplayName("POST /api/customers – COUCUS00 create customer")
    class CreateCustomerTests {

        @Test
        @DisplayName("should return 201 Created with new customer")
        void shouldCreateCustomerSuccessfully() throws Exception {
            given(customerService.createCustomer(any(CustomerData.class)))
                    .willReturn(sampleCustomer);

            mockMvc.perform(post("/api/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleCustomer)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId", is(100000001)))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.primaryCardHolderInd", is("Y")));

            verify(customerService).createCustomer(any(CustomerData.class));
        }

        @Test
        @DisplayName("should return 409 Conflict when customer ID already exists")
        void shouldReturn409WhenDuplicateId() throws Exception {
            given(customerService.createCustomer(any(CustomerData.class)))
                    .willThrow(new IllegalArgumentException(
                            "Customer already exists with id=100000001"));

            mockMvc.perform(post("/api/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleCustomer)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // PUT /api/customers/{customerId} – COUCUSUPD
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/customers/{customerId} – COUCUSUPD update customer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("should return 200 OK with updated customer")
        void shouldUpdateCustomerSuccessfully() throws Exception {
            CustomerData updatedCustomer = CustomerData.builder()
                    .customerId(100000001L)
                    .firstName("John")
                    .middleName("B")
                    .lastName("Doe")
                    .addrLine1("456 Oak Ave")
                    .addrStateCd("CA")
                    .addrCountryCd("USA")
                    .addrZip("90210")
                    .ficoCreditScore(750)
                    .primaryCardHolderInd("Y")
                    .build();

            given(customerService.updateCustomer(eq(100000001L), any(CustomerData.class)))
                    .willReturn(updatedCustomer);

            mockMvc.perform(put("/api/customers/100000001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedCustomer)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId", is(100000001)))
                    .andExpect(jsonPath("$.addrStateCd", is("CA")))
                    .andExpect(jsonPath("$.ficoCreditScore", is(750)));

            verify(customerService).updateCustomer(eq(100000001L), any(CustomerData.class));
        }

        @Test
        @DisplayName("should return 404 Not Found when customer to update does not exist")
        void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
            given(customerService.updateCustomer(anyLong(), any(CustomerData.class)))
                    .willThrow(new EntityNotFoundException("Customer not found with id=999999999"));

            mockMvc.perform(put("/api/customers/999999999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleCustomer)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
