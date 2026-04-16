package com.carddemo.service;

import com.carddemo.dto.UserDeleteResponse;
import com.carddemo.exception.UserDeleteNotAllowedException;
import com.carddemo.exception.UserNotFoundException;
import com.carddemo.model.User;
import com.carddemo.model.UserStatus;
import com.carddemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserDeleteService} — migration of COUSR03C.CBL.
 *
 * <p>Each nested class targets one logical paragraph from the original COBOL program.
 * Test names are deliberately verbose to document the COBOL-to-Java mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDeleteService — COUSR03C migration tests")
class UserDeleteServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDeleteService userDeleteService;

    // -----------------------------------------------------------------------
    // Shared fixtures
    // -----------------------------------------------------------------------

    private User activeRegularUser;
    private User activeAdminUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeRegularUser = User.builder()
                .userId("JOHN0001")
                .firstName("John")
                .lastName("Doe")
                .userType("U")
                .status(UserStatus.ACTIVE)
                .build();

        activeAdminUser = User.builder()
                .userId("ADMIN001")
                .firstName("Alice")
                .lastName("Admin")
                .userType("A")
                .status(UserStatus.ACTIVE)
                .build();

        inactiveUser = User.builder()
                .userId("GONE0001")
                .firstName("Gone")
                .lastName("User")
                .userType("U")
                .status(UserStatus.INACTIVE)
                .build();
    }

    // =======================================================================
    // PROCESS-ENTER-KEY equivalent — lookupUser()
    // =======================================================================

    @Nested
    @DisplayName("lookupUser() — COBOL PROCESS-ENTER-KEY paragraph")
    class LookupUserTests {

        @Test
        @DisplayName("returns user when ID is found and ACTIVE (DFHRESP NORMAL)")
        void lookupUser_existingActiveUser_returnsUser() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));

            User result = userDeleteService.lookupUser("JOHN0001");

            assertThat(result).isEqualTo(activeRegularUser);
            verify(userRepository).findById("JOHN0001");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user ID not found — COBOL DFHRESP(NOTFND): 'User ID NOT found...'")
        void lookupUser_notFound_throwsUserNotFoundException() {
            when(userRepository.findById("MISSING1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDeleteService.lookupUser("MISSING1"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("NOT found");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user is already INACTIVE — already-deleted record is invisible like a physical VSAM delete")
        void lookupUser_inactiveUser_throwsUserNotFoundException() {
            when(userRepository.findById("GONE0001")).thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> userDeleteService.lookupUser("GONE0001"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for blank userId — COBOL: 'User ID can NOT be empty...'")
        void lookupUser_blankId_throwsIllegalArgument() {
            assertThatThrownBy(() -> userDeleteService.lookupUser(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("can NOT be empty");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null userId — COBOL: 'User ID can NOT be empty...'")
        void lookupUser_nullId_throwsIllegalArgument() {
            assertThatThrownBy(() -> userDeleteService.lookupUser(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("can NOT be empty");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for whitespace-only userId — COBOL SPACES guard")
        void lookupUser_whitespaceId_throwsIllegalArgument() {
            assertThatThrownBy(() -> userDeleteService.lookupUser("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("userId is trimmed before repository lookup")
        void lookupUser_paddedUserId_isTrimmedForLookup() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));

            User result = userDeleteService.lookupUser("JOHN0001  "); // 8-char padded like COBOL PIC X(08)

            assertThat(result).isEqualTo(activeRegularUser);
            verify(userRepository).findById("JOHN0001");
        }
    }

    // =======================================================================
    // DELETE-USER-INFO / DELETE-USER-SEC-FILE equivalent — deleteUser()
    // =======================================================================

    @Nested
    @DisplayName("deleteUser() — COBOL DELETE-USER-INFO + DELETE-USER-SEC-FILE paragraphs")
    class DeleteUserTests {

        @Test
        @DisplayName("successfully soft-deletes a regular user and returns COBOL-format success message")
        void deleteUser_regularUser_softDeletesAndReturnsSuccessMessage() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserDeleteResponse response = userDeleteService.deleteUser("JOHN0001");

            assertThat(response.userId()).isEqualTo("JOHN0001");
            assertThat(response.message()).contains("JOHN0001").contains("has been deleted ...");
            assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("sets user status to INACTIVE on soft delete — migration of CICS physical DELETE")
        void deleteUser_setsStatusToInactive() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            userDeleteService.deleteUser("JOHN0001");

            User saved = userCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("sets deletedAt audit timestamp when soft-deleting")
        void deleteUser_setsDeletedAtTimestamp() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            userDeleteService.deleteUser("JOHN0001");

            assertThat(userCaptor.getValue().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist — COBOL DFHRESP(NOTFND): 'User ID NOT found...'")
        void deleteUser_userNotFound_throwsUserNotFoundException() {
            when(userRepository.findById("MISSING1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDeleteService.deleteUser("MISSING1"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("NOT found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user is already INACTIVE — idempotency safety")
        void deleteUser_alreadyInactiveUser_throwsUserNotFoundException() {
            when(userRepository.findById("GONE0001")).thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> userDeleteService.deleteUser("GONE0001"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for blank userId — COBOL: 'User ID can NOT be empty...'")
        void deleteUser_blankUserId_throwsIllegalArgument() {
            assertThatThrownBy(() -> userDeleteService.deleteUser(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("can NOT be empty");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null userId")
        void deleteUser_nullUserId_throwsIllegalArgument() {
            assertThatThrownBy(() -> userDeleteService.deleteUser(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("userId is trimmed before lookup — handles COBOL PIC X(08) trailing-space padding")
        void deleteUser_paddedUserId_isTrimmed() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDeleteResponse response = userDeleteService.deleteUser("JOHN0001  ");

            assertThat(response).isNotNull();
            verify(userRepository).findById("JOHN0001");
        }
    }

    // =======================================================================
    // Last-admin guard — RACF implicit protection made explicit in Java
    // =======================================================================

    @Nested
    @DisplayName("Last-admin cascade guard — replaces implicit RACF/CICS protection")
    class AdminGuardTests {

        @Test
        @DisplayName("throws UserDeleteNotAllowedException when deleting the sole remaining admin")
        void deleteUser_lastAdmin_throwsUserDeleteNotAllowedException() {
            when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(activeAdminUser));
            when(userRepository.countByUserTypeAndStatus("A", UserStatus.ACTIVE)).thenReturn(1L);

            assertThatThrownBy(() -> userDeleteService.deleteUser("ADMIN001"))
                    .isInstanceOf(UserDeleteNotAllowedException.class)
                    .hasMessageContaining("last active administrator");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows deletion of admin when other admins exist (count > 1)")
        void deleteUser_adminWithPeerAdmins_succeedsDelete() {
            when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(activeAdminUser));
            when(userRepository.countByUserTypeAndStatus("A", UserStatus.ACTIVE)).thenReturn(3L);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDeleteResponse response = userDeleteService.deleteUser("ADMIN001");

            assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("does NOT check admin count for regular (non-admin) users")
        void deleteUser_regularUser_doesNotCheckAdminCount() {
            when(userRepository.findById("JOHN0001")).thenReturn(Optional.of(activeRegularUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userDeleteService.deleteUser("JOHN0001");

            verify(userRepository, never()).countByUserTypeAndStatus(any(), any());
        }

        @Test
        @DisplayName("admin constant is 'A' — matches COBOL SEC-USR-TYPE value")
        void adminUserTypeConstant_isLetterA() {
            assertThat(UserDeleteService.ADMIN_USER_TYPE).isEqualTo("A");
        }
    }

    // =======================================================================
    // Success message format — mirrors COBOL STRING statement
    // =======================================================================

    @Nested
    @DisplayName("Success message format — mirrors COBOL STRING statement in DELETE-USER-SEC-FILE")
    class SuccessMessageTests {

        @Test
        @DisplayName("message matches COBOL format: 'User <id> has been deleted ...'")
        void deleteUser_successMessage_matchesCobolFormat() {
            when(userRepository.findById("SMITH001")).thenReturn(Optional.of(
                    User.builder().userId("SMITH001").userType("U").status(UserStatus.ACTIVE).build()));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDeleteResponse response = userDeleteService.deleteUser("SMITH001");

            // COBOL: STRING 'User ' ... SEC-USR-ID DELIMITED BY SPACE ... ' has been deleted ...'
            assertThat(response.message()).isEqualTo("User SMITH001 has been deleted ...");
        }

        @Test
        @DisplayName("trailing spaces in userId are trimmed in the success message — COBOL DELIMITED BY SPACE")
        void deleteUser_paddedUserId_messageTrimsSpaces() {
            // Service trims userId before findById, so repository is called with "ABC"
            when(userRepository.findById("ABC")).thenReturn(Optional.of(
                    User.builder().userId("ABC").userType("U").status(UserStatus.ACTIVE).build()));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDeleteResponse response = userDeleteService.deleteUser("ABC     ");

            // COBOL STRING uses DELIMITED BY SPACE, so trailing spaces are dropped
            assertThat(response.message()).contains("User ABC has been deleted ...");
        }
    }
}
