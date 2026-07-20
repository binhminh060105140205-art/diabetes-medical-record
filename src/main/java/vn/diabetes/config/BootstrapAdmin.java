package vn.diabetes.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.diabetes.auth.Passwords;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final Environment environment;

    public BootstrapAdmin(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty("BOOTSTRAP_ADMIN_USERNAME", "admin");
        String password = environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD");
        String name = environment.getProperty("BOOTSTRAP_ADMIN_NAME", "Quản trị hệ thống");
        if (password == null || password.isBlank()) {
            return;
        }
        jdbc.update("""
                INSERT INTO users(username,password,full_name,role,status)
                VALUES (?,?,?,?,?)
                ON CONFLICT (username) DO UPDATE SET
                  password=EXCLUDED.password,
                  full_name=EXCLUDED.full_name,
                  role='ADMIN',
                  status='ACTIVE'
                WHERE users.password IS DISTINCT FROM EXCLUDED.password
                   OR users.full_name IS DISTINCT FROM EXCLUDED.full_name
                   OR users.role IS DISTINCT FROM 'ADMIN'
                   OR users.status IS DISTINCT FROM 'ACTIVE'
                """, username, Passwords.encode(password), name, "ADMIN", "ACTIVE");
    }
}
