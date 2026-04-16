package com.carddemo.security;

import com.carddemo.model.User;
import com.carddemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security {@link UserDetailsService} for CardDemo.
 *
 * <p>This service replaces the RACF USRSEC dataset lookup that was performed by
 * COSGN00C.cbl:
 * <pre>
 *   EXEC CICS READ
 *        DATASET   (WS-USRSEC-FILE)   -- 'USRSEC  '
 *        INTO      (SEC-USER-DATA)
 *        RIDFLD    (SEC-USR-ID)
 *        ...
 *   END-EXEC.
 *   IF SEC-USR-PWD = WS-USER-PWD
 *       MOVE SEC-USR-TYPE TO CDEMO-USER-TYPE
 * </pre>
 *
 * <p><b>COBOL role-code mapping</b> (verified from COCOM01Y.cpy):
 * <ul>
 *   <li>{@code 'A'} — {@code CDEMO-USRTYP-ADMIN VALUE 'A'} → {@code ROLE_ADMIN}</li>
 *   <li>{@code 'U'} — {@code CDEMO-USRTYP-USER  VALUE 'U'} → {@code ROLE_USER}</li>
 * </ul>
 *
 * <p>Any unrecognised type defaults to {@code ROLE_USER} to follow the
 * principle of least privilege.
 */
@Service
@Transactional(readOnly = true)
public class CardDemoUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CardDemoUserDetailsService.class);

    /**
     * COBOL: CDEMO-USRTYP-ADMIN VALUE 'A' (COCOM01Y.cpy line 27).
     */
    public static final String COBOL_TYPE_ADMIN = "A";

    /**
     * COBOL: CDEMO-USRTYP-USER VALUE 'U' (COCOM01Y.cpy line 28).
     */
    public static final String COBOL_TYPE_USER = "U";

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER  = "ROLE_USER";

    private final UserRepository userRepository;

    public CardDemoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a {@link UserDetails} by username (SEC-USR-ID).
     *
     * @param username the 8-character user ID
     * @return populated {@link UserDetails} including mapped roles
     * @throws UsernameNotFoundException if no user is found (mirrors CICS NOTFND response)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> {
                    log.warn("User not found: '{}' (COBOL: CICS NOTFND response for USRSEC READ)",
                            username);
                    return new UsernameNotFoundException(
                            "User not found in USRSEC: " + username);
                });

        log.debug("Loaded user '{}' with COBOL type '{}'", user.getUserId(), user.getUserType());

        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                mapRoles(user.getUserType()));
    }

    /**
     * Maps the COBOL SEC-USR-TYPE single-character code to Spring
     * {@link GrantedAuthority} objects.
     *
     * <p>Mapping table (COCOM01Y.cpy):
     * <pre>
     *   'A' → ROLE_ADMIN
     *   'U' → ROLE_USER   (and default for any unrecognised value)
     * </pre>
     *
     * @param cobolUserType the single-character type from SEC-USR-TYPE
     * @return immutable list of granted authorities
     */
    public static Collection<? extends GrantedAuthority> mapRoles(String cobolUserType) {
        if (COBOL_TYPE_ADMIN.equalsIgnoreCase(cobolUserType)) {
            // Admin users receive both ROLE_ADMIN and ROLE_USER authorities
            return List.of(
                    new SimpleGrantedAuthority(ROLE_ADMIN),
                    new SimpleGrantedAuthority(ROLE_USER));
        }
        // Default: ROLE_USER (covers 'U' and any unrecognised type codes)
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }
}
