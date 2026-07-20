package vn.diabetes.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import models.User;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    @Test
    void authenticatesEncodedPasswordAndClearsPreviousFailures() {
        UserRepository repository = mock(UserRepository.class);
        User user = userWithPassword(1, Passwords.encode("Staff@123"));
        when(repository.findActiveByUsername("staff01")).thenReturn(Optional.of(user));
        when(repository.getLoginSecurityState(1))
                .thenReturn(new UserRepository.LoginSecurityState(2, null));

        AuthenticationService.LoginResult result =
                new AuthenticationService(repository).login("staff01", "Staff@123");

        assertTrue(result.successful());
        assertEquals(user, result.user());
        verify(repository).clearLoginFailures(1);
    }

    @Test
    void rejectsUnknownAccountWithoutExposingRemainingAttempts() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findActiveByUsername("missing")).thenReturn(Optional.empty());

        AuthenticationService.LoginResult result =
                new AuthenticationService(repository).login("missing", "wrong");

        assertFalse(result.successful());
        assertNotNull(result.error());
        assertEquals(null, result.remainingAttempts());
    }

    @Test
    void reportsRemainingAttemptsAfterWrongPassword() {
        UserRepository repository = mock(UserRepository.class);
        User user = userWithPassword(7, "correct-password");
        when(repository.findActiveByUsername("doctor01")).thenReturn(Optional.of(user));
        when(repository.getLoginSecurityState(7))
                .thenReturn(new UserRepository.LoginSecurityState(2, null));
        when(repository.recordFailedLogin(eq(7), eq(5), any(Instant.class)))
                .thenReturn(new UserRepository.LoginSecurityState(3, null));

        AuthenticationService.LoginResult result =
                new AuthenticationService(repository).login("doctor01", "wrong-password");

        assertFalse(result.successful());
        assertEquals(2, result.remainingAttempts());
        assertTrue(result.error().contains("2 lần thử"));
    }

    @Test
    void locksAccountOnFifthWrongPassword() {
        UserRepository repository = mock(UserRepository.class);
        User user = userWithPassword(9, "correct-password");
        Instant lockUntil = Instant.now().plusSeconds(900);
        when(repository.findActiveByUsername("admin01")).thenReturn(Optional.of(user));
        when(repository.getLoginSecurityState(9))
                .thenReturn(new UserRepository.LoginSecurityState(4, null));
        when(repository.recordFailedLogin(eq(9), eq(5), any(Instant.class)))
                .thenReturn(new UserRepository.LoginSecurityState(5, lockUntil));

        AuthenticationService.LoginResult result =
                new AuthenticationService(repository).login("admin01", "wrong-password");

        assertTrue(result.locked());
        assertEquals(lockUntil, result.lockUntil());
        assertEquals(0, result.remainingAttempts());
    }

    @Test
    void rejectsPasswordWhileAccountIsLocked() {
        UserRepository repository = mock(UserRepository.class);
        User user = userWithPassword(4, "correct-password");
        Instant lockUntil = Instant.now().plusSeconds(600);
        when(repository.findActiveByUsername("staff01")).thenReturn(Optional.of(user));
        when(repository.getLoginSecurityState(4))
                .thenReturn(new UserRepository.LoginSecurityState(5, lockUntil));

        AuthenticationService.LoginResult result =
                new AuthenticationService(repository).login("staff01", "correct-password");

        assertTrue(result.locked());
        verify(repository, never()).recordFailedLogin(eq(4), eq(5), any(Instant.class));
    }

    @Test
    void validatesBlankAndOversizedLoginInputBeforeDatabaseLookup() {
        UserRepository repository = mock(UserRepository.class);
        AuthenticationService service = new AuthenticationService(repository);

        assertFalse(service.login("", "password").successful());
        assertFalse(service.login("abc", "password").successful());
        assertFalse(service.login("staff01", " ").successful());
        assertFalse(service.login("staff01", "x".repeat(73)).successful());
        verify(repository, never()).findActiveByUsername(any());
    }

    private User userWithPassword(int userId, String password) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("account" + userId);
        user.setPassword(password);
        return user;
    }
}
