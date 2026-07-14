package vn.diabetes.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.diabetes.auth.Passwords;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final Environment environment;

    public BootstrapAdmin(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (users != null && users > 0) return;

        String username = environment.getProperty("BOOTSTRAP_ADMIN_USERNAME", "admin");
        String password = environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD");
        String name = environment.getProperty("BOOTSTRAP_ADMIN_NAME", "System Administrator");
        if (password == null || password.isBlank() || "change-this-now".equals(password)) {
            throw new IllegalStateException("Set a strong BOOTSTRAP_ADMIN_PASSWORD for the first deployment");
        }
        jdbc.update("INSERT INTO users(username,password,full_name,role,status) VALUES (?,?,?,?,?)",
                username, Passwords.encode(password), name, "ADMIN", "ACTIVE");
    }
}
