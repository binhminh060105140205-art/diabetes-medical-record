package vn.diabetes.web;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RootController {
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
}
