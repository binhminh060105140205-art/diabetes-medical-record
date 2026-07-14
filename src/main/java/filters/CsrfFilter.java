package filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/** Lightweight CSRF protection shared by all JSP/Servlet forms. */
@WebFilter("/*")
public class CsrfFilter implements Filter {
    public static final String SESSION_KEY = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> UNSAFE = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        HttpSession session = request.getSession(true);
        String expected = (String) session.getAttribute(SESSION_KEY);
        if (expected == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            expected = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_KEY, expected);
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (UNSAFE.contains(request.getMethod()) && !"/api/device-data/upload".equals(path)) {
            String actual = request.getHeader("X-CSRF-Token");
            if (actual == null || actual.isBlank()) actual = request.getParameter("_csrf");
            if (actual == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Yêu cầu không hợp lệ hoặc phiên đã hết hạn.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
