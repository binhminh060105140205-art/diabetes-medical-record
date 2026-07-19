package vn.diabetes.web;

import org.springframework.stereotype.Controller;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RootController {
    private final JdbcClient jdbc;

    public RootController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/")
    public String home() {
        return "forward:/index.jsp";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        String revision = System.getenv().getOrDefault("RENDER_GIT_COMMIT", "local");
        if (revision.length() > 8) revision = revision.substring(0, 8);
        return ResponseEntity.ok("OK revision=" + revision);
    }

    /** Render only routes users to a new instance after PostgreSQL is ready. */
    @GetMapping("/ready")
    @ResponseBody
    public ResponseEntity<String> ready() {
        try {
            Integer result = jdbc.sql("SELECT 1").query(Integer.class).single();
            return result != null && result == 1
                    ? ResponseEntity.ok("READY")
                    : ResponseEntity.status(503).body("NOT_READY");
        } catch (DataAccessException error) {
            return ResponseEntity.status(503).body("DATABASE_NOT_READY");
        }
    }
}
