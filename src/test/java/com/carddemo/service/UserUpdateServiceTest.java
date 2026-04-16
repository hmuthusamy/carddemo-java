package com.carddemo.service;

import com.carddemo.dto.UserUpdateRequest;
import com.carddemo.dto.UserUpdateResponse;
import com.carddemo.exception.UserNotFoundException;
import com.carddemo.exception.UserValidationException;
import com.carddemo.model.UserSec;
import com.carddemo.repository.UserSecRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserUpdateService}.
 *
 * Covers every EVALUATE branch from COUSR02C UPDATE-USER-INFO
 * and PROCESS-ENTER-KEY paragraphs.
 *
 * No Spring context is loaded – pure Mockito wiring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserUpdateService – COUSR02C migration unit tests")
class UserUpdateServiceTest {

    @Mock
    private UserSecRepository userSecRepository;

    @InjectMocks
    private UserUpdateService userUpdateService;

    // ---------------------------------------------------------------
    // Common fixture
    // ---------------------------------------------------------------
    private UserSec existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new UserSec("JOHNDOE ", "John    ", "Doe     ", "pass1234", "R");
    }

    // ═══════════════════════════════════════════════════════════════
    // lookupUser (PROCESS-ENTER-KEY)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("lookupUser – PROCESS-ENTER-KEY paragraph")
    class LookupUserTests {

        @Test
        @DisplayName("returns user when userId exists in repository")
        void lookupUser_found_returnsEntity() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));

            UserSec result = userUpdateService.lookupUser("JOHNDOE");

            assertThat(result).isNotNull();
            assertThat(result.getUsrId()).isEqualTo("JOHNDOE ");
            verify(userSecRepository, times(1)).findById("JOHNDOE");
        }

        @Test
        @DisplayName("throws UserValidationException when userId is blank – WHEN USRIDINI = SPACES")
        void lookupUser_blankUserId_throwsValidation() {
            assertThatThrownBy(() -> userUpdateService.lookupUser("   "))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_USERID_EMPTY);
            verify(userSecRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws UserValidationException when userId is null")
        void lookupUser_nullUserId_throwsValidation() {
            assertThatThrownBy(() -> userUpdateService.lookupUser(null))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_USERID_EMPTY);
        }

        @Test
        @DisplayName("throws UserNotFoundException when userId not in USRSEC – DFHRESP(NOTFND)")
        void lookupUser_notFound_throwsNotFoundException() {
            when(userSecRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userUpdateService.lookupUser("UNKNOWN"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User ID NOT found");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // updateUser – UPDATE-USER-INFO paragraph validation (EVALUATE)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateUser – field-level validation (EVALUATE TRUE branches)")
    class UpdateUserValidationTests {

        @Test
        @DisplayName("throws when userId is blank – WHEN USRIDINI = SPACES")
        void updateUser_blankUserId_throwsValidation() {
            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "pass1234", "R");

            assertThatThrownBy(() -> userUpdateService.updateUser("  ", req))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_USERID_EMPTY);
            verify(userSecRepository, never()).findById(any());
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when firstName is blank – WHEN FNAMEI = SPACES")
        void updateUser_blankFirstName_throwsValidation() {
            UserUpdateRequest req = new UserUpdateRequest("  ", "Doe", "pass1234", "R");

            assertThatThrownBy(() -> userUpdateService.updateUser("JOHNDOE", req))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_FNAME_EMPTY);
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when lastName is blank – WHEN LNAMEI = SPACES")
        void updateUser_blankLastName_throwsValidation() {
            UserUpdateRequest req = new UserUpdateRequest("John", "  ", "pass1234", "R");

            assertThatThrownBy(() -> userUpdateService.updateUser("JOHNDOE", req))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_LNAME_EMPTY);
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when password is blank – WHEN PASSWDI = SPACES")
        void updateUser_blankPassword_throwsValidation() {
            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "   ", "R");

            assertThatThrownBy(() -> userUpdateService.updateUser("JOHNDOE", req))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_PASSWORD_EMPTY);
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when userType is blank – WHEN USRTYPEI = SPACES")
        void updateUser_blankUserType_throwsValidation() {
            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "pass1234", "  ");

            assertThatThrownBy(() -> userUpdateService.updateUser("JOHNDOE", req))
                    .isInstanceOf(UserValidationException.class)
                    .hasMessage(UserUpdateService.MSG_USERTYPE_EMPTY);
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user not found – DFHRESP(NOTFND)")
        void updateUser_userNotFound_throwsNotFoundException() {
            when(userSecRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "pass1234", "R");

            assertThatThrownBy(() -> userUpdateService.updateUser("UNKNOWN", req))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User ID NOT found");
            verify(userSecRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // updateUser – USR-MODIFIED-YES / NO logic
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateUser – USR-MODIFIED flag and selective update logic")
    class UpdateUserModifiedTests {

        @Test
        @DisplayName("returns success and saves when firstName changed – USR-MODIFIED-YES")
        void updateUser_firstNameChanged_savesAndReturnsSuccess() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("Jane", "Doe", "pass1234", "R");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).contains("has been updated");
            assertThat(response.getUserId()).isEqualTo("JOHNDOE");
            verify(userSecRepository, times(1)).save(existingUser);
        }

        @Test
        @DisplayName("returns success and saves when lastName changed")
        void updateUser_lastNameChanged_savesAndReturnsSuccess() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("John", "Smith", "pass1234", "R");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isTrue();
            verify(userSecRepository).save(existingUser);
        }

        @Test
        @DisplayName("returns success and saves when password changed")
        void updateUser_passwordChanged_savesAndReturnsSuccess() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "newpass1", "R");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isTrue();
            verify(userSecRepository).save(existingUser);
        }

        @Test
        @DisplayName("returns success and saves when userType changed")
        void updateUser_userTypeChanged_savesAndReturnsSuccess() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "pass1234", "A");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isTrue();
            verify(userSecRepository).save(existingUser);
        }

        @Test
        @DisplayName("returns not-modified message when no fields changed – USR-MODIFIED-NO")
        void updateUser_noChanges_returnsNotModified() {
            // existingUser: fname="John    ", lname="Doe     ", pwd="pass1234", type="R"
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));

            // Request values that, after trim, equal stored values
            UserUpdateRequest req = new UserUpdateRequest("John", "Doe", "pass1234", "R");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo(UserUpdateService.MSG_NOTHING_CHANGED);
            verify(userSecRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves all four fields when all changed simultaneously")
        void updateUser_allFieldsChanged_savesAll() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("Jane", "Smith", "newpass1", "A");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.isSuccess()).isTrue();
            assertThat(existingUser.getUsrFname()).isEqualTo("Jane");
            assertThat(existingUser.getUsrLname()).isEqualTo("Smith");
            assertThat(existingUser.getUsrPwd()).isEqualTo("newpass1");
            assertThat(existingUser.getUsrType()).isEqualTo("A");
            verify(userSecRepository).save(existingUser);
        }

        @Test
        @DisplayName("success message contains userId – mirrors COBOL STRING paragraph")
        void updateUser_successMessage_containsUserId() {
            when(userSecRepository.findById("JOHNDOE")).thenReturn(Optional.of(existingUser));
            when(userSecRepository.save(any(UserSec.class))).thenReturn(existingUser);

            UserUpdateRequest req = new UserUpdateRequest("Jane", "Doe", "pass1234", "R");
            UserUpdateResponse response = userUpdateService.updateUser("JOHNDOE", req);

            assertThat(response.getMessage()).startsWith("User JOHNDOE has been updated");
        }
    }
}
