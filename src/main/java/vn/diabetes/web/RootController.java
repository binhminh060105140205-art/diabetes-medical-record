package vn.diabetes.web;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RootController {
    private final JdbcTemplate jdbc;

    public RootController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @GetMapping("/")
    public String home() {
        return "forward:/index.jsp";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        try {
            Integer value = jdbc.queryForObject("SELECT 1", Integer.class);
            String revision = System.getenv().getOrDefault("RENDER_GIT_COMMIT", "local");
            if (revision.length() > 8) revision = revision.substring(0, 8);
            return value != null && value == 1
                    ? ResponseEntity.ok("OK revision=" + revision)
                    : ResponseEntity.internalServerError().body("DATABASE_UNHEALTHY");
        } catch (Exception ex) {
            return ResponseEntity.status(503).body("DATABASE_UNAVAILABLE");
        }
    }
}
