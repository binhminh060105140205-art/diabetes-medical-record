package vn.diabetes.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import models.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = Duration.ofMinutes(5).toMillis();
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final UserRepository users;

    public AuthenticationService(UserRepository users) {
        this.users = users;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank()) return LoginResult.failed("Tên đăng nhập không được để trống");
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(username, ignored -> new Attempt());
        if (attempt.lockUntil > now) return LoginResult.locked(attempt.lockUntil);

        Optional<User> user = users.findActiveByUsername(username.trim());
        if (user.isPresent() && Passwords.matches(password, user.get().getPassword())) {
            if (!Passwords.isEncoded(user.get().getPassword())) {
                String encoded = Passwords.encode(password);
                users.updatePassword(user.get().getUserId(), encoded);
                user.get().setPassword(null);
            }
            attempts.remove(username);
            return LoginResult.success(user.get());
        }

        synchronized (attempt) {
            attempt.count++;
            if (attempt.count >= MAX_ATTEMPTS) attempt.lockUntil = now + LOCK_MILLIS;
            return attempt.lockUntil > now
                    ? LoginResult.locked(attempt.lockUntil)
                    : LoginResult.failed("Sai tên đăng nhập hoặc mật khẩu. Lần thử: " + attempt.count + "/" + MAX_ATTEMPTS);
        }
    }

    private static final class Attempt { int count; long lockUntil; }

    public record LoginResult(User user, String error, Long lockUntil) {
        static LoginResult success(User user) { return new LoginResult(user, null, null); }
        static LoginResult failed(String error) { return new LoginResult(null, error, null); }
        static LoginResult locked(long until) { return new LoginResult(null, "Tài khoản tạm khóa trong 5 phút.", until); }
        public boolean successful() { return user != null; }
    }
}
