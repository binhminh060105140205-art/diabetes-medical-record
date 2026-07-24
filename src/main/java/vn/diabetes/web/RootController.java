package vn.diabetes.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
    @GetMapping("/")
    public String home() {
        return "forward:/index.jsp";
    }
}
