package vn.diabetes.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** Uses the existing Vietnamese JSP instead of Spring Boot's Whitelabel page. */
@Controller
public class ApplicationErrorController implements ErrorController {
    @RequestMapping("/error")
    public String error() {
        return "forward:/error.jsp";
    }
}
