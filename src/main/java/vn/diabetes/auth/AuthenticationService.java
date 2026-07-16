package vn.diabetes.auth;

import java.util.Optional;
import models.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserRepository users;

    public AuthenticationService(UserRepository users) {
        this.users = users;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank()) return LoginResult.failed("Tên đăng nhập không được để trống");
        Optional<User> user = users.findActiveByUsername(username.trim());
        if (user.isPresent() && Passwords.matches(password, user.get().getPassword())) {
            if (Passwords.needsRehash(user.get().getPassword())) {
                users.updatePassword(user.get().getUserId(), password);
            }
            user.get().setPassword(null);
            return LoginResult.success(user.get());
        }
        return LoginResult.failed("Sai tên đăng nhập hoặc mật khẩu.");
    }

    public record LoginResult(User user, String error) {
        static LoginResult success(User user) { return new LoginResult(user, null); }
        static LoginResult failed(String error) { return new LoginResult(null, error); }
        public boolean successful() { return user != null; }
    }
}
