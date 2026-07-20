package vn.diabetes.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import models.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String INVALID_CREDENTIALS = "Sai tên đăng nhập hoặc mật khẩu.";
    private final UserRepository users;

    public AuthenticationService(UserRepository users) {
        this.users = users;
    }

    public LoginResult login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty()) {
            return LoginResult.failed("Tên đăng nhập không được để trống.");
        }
        if (normalizedUsername.length() < 4 || normalizedUsername.length() > 50
                || !normalizedUsername.matches("^[A-Za-z0-9._-]+$")) {
            return LoginResult.failed("Tên đăng nhập không hợp lệ.");
        }
        if (password == null || password.isBlank()) {
            return LoginResult.failed("Mật khẩu không được để trống.");
        }
        if (password.length() > 72) {
            return LoginResult.failed("Mật khẩu không hợp lệ.");
        }

        Optional<User> foundUser = users.findActiveByUsername(normalizedUsername);
        if (foundUser.isEmpty()) return LoginResult.failed(INVALID_CREDENTIALS);

        User user = foundUser.get();
        Instant now = Instant.now();
        UserRepository.LoginSecurityState security = users.getLoginSecurityState(user.getUserId());
        if (security == null) security = new UserRepository.LoginSecurityState(0, null);

        if (security.lockUntil() != null && security.lockUntil().isAfter(now)) {
            return LoginResult.locked(security.lockUntil());
        }
        if (security.lockUntil() != null) {
            users.clearLoginFailures(user.getUserId());
            security = new UserRepository.LoginSecurityState(0, null);
        }

        if (Passwords.matches(password, user.getPassword())) {
            if (security.failedAttempts() > 0) users.clearLoginFailures(user.getUserId());
            if (Passwords.needsRehash(user.getPassword())) {
                users.updatePassword(user.getUserId(), password);
            }
            user.setPassword(null);
            return LoginResult.success(user);
        }

        Instant lockUntil = now.plus(LOCK_DURATION);
        UserRepository.LoginSecurityState updated = users.recordFailedLogin(
                user.getUserId(), MAX_FAILED_ATTEMPTS, lockUntil);
        if (updated.lockUntil() != null || updated.failedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return LoginResult.locked(updated.lockUntil() == null ? lockUntil : updated.lockUntil());
        }
        int remaining = Math.max(0, MAX_FAILED_ATTEMPTS - updated.failedAttempts());
        return LoginResult.failed(INVALID_CREDENTIALS + " Bạn còn " + remaining + " lần thử.", remaining);
    }

    public record LoginResult(User user, String error, Instant lockUntil, Integer remainingAttempts) {
        static LoginResult success(User user) { return new LoginResult(user, null, null, null); }
        static LoginResult failed(String error) { return new LoginResult(null, error, null, null); }
        static LoginResult failed(String error, int remainingAttempts) {
            return new LoginResult(null, error, null, remainingAttempts);
        }
        static LoginResult locked(Instant lockUntil) {
            return new LoginResult(null,
                    "Bạn đã nhập sai 5 lần. Tài khoản tạm khóa trong 15 phút.",
                    lockUntil, 0);
        }
        public boolean successful() { return user != null; }
        public boolean locked() { return lockUntil != null; }
    }
}
