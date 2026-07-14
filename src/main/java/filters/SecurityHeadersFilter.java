package filters;

import dal.DBContext;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse http = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        http.setHeader("X-Content-Type-Options", "nosniff");
        http.setHeader("X-Frame-Options", "DENY");
        http.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        http.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (httpRequest.getRequestURI().contains("/static/")) {
            http.setHeader("Cache-Control", "public, max-age=86400");
        }
        DBContext.beginRequest();
        try {
            chain.doFilter(request, response);
        } finally {
            DBContext.closeRequestConnections();
        }
    }
}
