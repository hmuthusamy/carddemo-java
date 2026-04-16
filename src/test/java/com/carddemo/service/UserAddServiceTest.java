package com.carddemo.service;

import com.carddemo.model.UserData;
import com.carddemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserAddService.
 *
 * Tests reproduce every validation rule from COUSR01C paragraph
 * PROCESS-ENTER-KEY and the duplicate-key behaviour of WRITE-USER-SEC-FILE.
 *
 * COBOL traceability:
 * Test 1:  Happy path - PROCESS-ENTER-KEY WHEN OTHER, WRITE-USER-SEC-FILE NORMAL
 * Test 2:  Duplicate userId - WRITE-USER-SEC-FILE DFHRESP(DUPKEY)/DFHRESP(DUPREC)
 * Test 3:  Blank firstName - WHEN FNAMEI = SPACES OR LOW-VALUES
 * Test 4:  Null firstName - LOW-VALUES equivalent
 * Test 5:  Blank lastName - WHEN LNAMEI = SPACES OR LOW-VALUES
 * Test 6:  Blank userId - WHEN USERIDI = SPACES OR LOW-VALUES
 * Test 7:  Blank password - WHEN PASSWDI = SPACES OR LOW-VALUES
 * Test 8:  Blank userType - WHEN USRTYPEI = SPACES OR LOW-VALUES
 * Test 9:  Invalid userType - SEC-USR-TYPE PIC X(01) must be R or A (CSUSR01Y)
 * Test 10: userId > 8 chars - SEC-USR-ID PIC X(08) field-length overflow
 * Test 11: Admin userType A - SEC-USR-TYPE A = Admin path
 * Test 12: Lowercase r normalised to R - COBOL UPPER-CASE semantics
 * Test 13: Password as BCrypt hash - intentional deviation SEC-USR-PWD PIC X(08)
 * Test 14: Validation order firstName before lastName (EVALUATE WHEN sequence)
 * Test 15: save() never called on validation failure (IF NOT ERR-FLG-ON guard)
 */
@ExtendWith(MockitoExtension.class)
class UserAddServiceTest {

    @Mock
    private UserRepository userRepository;

    // Real encoder - verify that BCrypt encoding actually occurs
    private BCryptPasswordEncoder passwordEncoder;

    private UserAddService userAddService;

    // Reusable valid inputs matching COBOL CSUSR01Y copybook field sizes
    private static final String VALID_USER_ID   = "JDOE";      // <= PIC X(08)
    private static final String VALID_FNAME     = "John";      // <= PIC X(20)
    private static final String VALID_LNAME     = "Doe";       // <= PIC X(20)
    private static final String VALID_PASSWORD  = "Passw0rd";  // PIC X(08)
    private static final String VALID_USER_TYPE = "R";         // PIC X(01)

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userAddService  = new UserAddService(userRepository, passwordEncoder);
    }

    // Test 1: Happy path
    @Test
    @DisplayName("1. addUser - valid input persists user (COBOL WRITE-USER-SEC-FILE DFHRESP NORMAL)")
    void addUser_validInput_persistsUser() {
        when(userRepository.existsByUserId(VALID_USER_ID)).thenReturn(false);
        when(userRepository.save(any(UserData.class))).thenAnswer(inv -> inv.getArgument(0));

        UserData result = userAddService.addUser(
                VALID_USER_ID, VALID_FNAME, VALID_LNAME, VALID_PASSWORD, VALID_USER_TYPE);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(VALID_USER_ID);
        assertThat(result.getFirstName()).isEqualTo(VALID_FNAME);
        assertThat(result.getLastName()).isEqualTo(VALID_LNAME);
        assertThat(result.getUserType()).isEqualTo("R");
        assertThat(result.isActive()).isTrue();
        verify(userRepository, times(1)).save(any(UserData.class));
    }

    // Test 2: Duplicate user (DFHRESP DUPKEY / DUPREC)
    @Test
    @DisplayName("2. addUser - duplicate userId throws UserAlreadyExistsException (DFHRESP DUPKEY)")
    void addUser_duplicateUserId_throwsUserAlreadyExistsException() {
        when(userRepository.existsByUserId(VALID_USER_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User ID already exist");

        verify(userRepository, never()).save(any());
    }

    // Test 3: Blank firstName
    @Test
    @DisplayName("3. addUser - blank firstName throws IllegalArgumentException (COUSR01C PROCESS-ENTER-KEY)")
    void addUser_blankFirstName_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, "   ", VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First Name can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
        verify(userRepository, never()).save(any());
    }

    // Test 4: Null firstName
    @Test
    @DisplayName("4. addUser - null firstName throws IllegalArgumentException (COBOL LOW-VALUES)")
    void addUser_nullFirstName_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, null, VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First Name can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
    }

    // Test 5: Blank lastName
    @Test
    @DisplayName("5. addUser - blank lastName throws IllegalArgumentException (COUSR01C PROCESS-ENTER-KEY)")
    void addUser_blankLastName_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, VALID_FNAME, "",
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Last Name can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
    }

    // Test 6: Blank userId
    @Test
    @DisplayName("6. addUser - blank userId throws IllegalArgumentException (COUSR01C PROCESS-ENTER-KEY)")
    void addUser_blankUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser("", VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
    }

    // Test 7: Blank password
    @Test
    @DisplayName("7. addUser - blank password throws IllegalArgumentException (COUSR01C PROCESS-ENTER-KEY)")
    void addUser_blankPassword_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME,
                                       "   ", VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
    }

    // Test 8: Blank userType
    @Test
    @DisplayName("8. addUser - blank userType throws IllegalArgumentException (COUSR01C PROCESS-ENTER-KEY)")
    void addUser_blankUserType_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User Type can NOT be empty");
        verify(userRepository, never()).existsByUserId(any());
    }

    // Test 9: Invalid userType
    @Test
    @DisplayName("9. addUser - invalid userType X throws IllegalArgumentException (SEC-USR-TYPE PIC X(01))")
    void addUser_invalidUserType_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, "X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User Type must be");
    }

    // Test 10: userId > 8 chars
    @Test
    @DisplayName("10. addUser - userId > 8 chars throws IllegalArgumentException (SEC-USR-ID PIC X(08))")
    void addUser_userIdTooLong_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                userAddService.addUser("TOOLONGID", VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must not exceed 8 characters");
    }

    // Test 11: Admin userType A accepted
    @Test
    @DisplayName("11. addUser - userType A (Admin) is accepted")
    void addUser_adminUserType_isAccepted() {
        when(userRepository.existsByUserId(VALID_USER_ID)).thenReturn(false);
        when(userRepository.save(any(UserData.class))).thenAnswer(inv -> inv.getArgument(0));

        UserData result = userAddService.addUser(
                VALID_USER_ID, VALID_FNAME, VALID_LNAME, VALID_PASSWORD, "A");

        assertThat(result.getUserType()).isEqualTo("A");
        verify(userRepository).save(any(UserData.class));
    }

    // Test 12: Lowercase r normalised to R
    @Test
    @DisplayName("12. addUser - userType r (lowercase) normalised to R (COBOL UPPER-CASE semantics)")
    void addUser_lowercaseUserType_normalisedAndAccepted() {
        when(userRepository.existsByUserId(VALID_USER_ID)).thenReturn(false);
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME, VALID_PASSWORD, "r");

        assertThat(captor.getValue().getUserType()).isEqualTo("R");
    }

    // Test 13: Password stored as BCrypt hash
    @Test
    @DisplayName("13. addUser - password stored as BCrypt hash NOT plain text (deviation: SEC-USR-PWD PIC X(08))")
    void addUser_passwordIsEncodedBeforePersistence() {
        when(userRepository.existsByUserId(VALID_USER_ID)).thenReturn(false);
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userAddService.addUser(VALID_USER_ID, VALID_FNAME, VALID_LNAME,
                               VALID_PASSWORD, VALID_USER_TYPE);

        UserData persisted = captor.getValue();
        assertThat(persisted.getPasswordHash())
                .as("Password must not be stored as plain text")
                .isNotEqualTo(VALID_PASSWORD);
        assertThat(passwordEncoder.matches(VALID_PASSWORD, persisted.getPasswordHash()))
                .as("BCrypt hash must match the original password")
                .isTrue();
    }

    // Test 14: Validation order - firstName before lastName
    @Test
    @DisplayName("14. addUser - firstName validated before lastName (COBOL EVALUATE WHEN sequence)")
    void addUser_validationOrder_firstNameBeforeLastName() {
        assertThatThrownBy(() ->
                userAddService.addUser(VALID_USER_ID, "", "",
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First Name can NOT be empty");
    }

    // Test 15: save() never called on validation failure
    @Test
    @DisplayName("15. addUser - save() never called when validation fails (COBOL IF NOT ERR-FLG-ON guard)")
    void addUser_saveNeverCalledOnValidationFailure() {
        assertThatThrownBy(() ->
                userAddService.addUser(null, VALID_FNAME, VALID_LNAME,
                                       VALID_PASSWORD, VALID_USER_TYPE))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository);
    }
}
