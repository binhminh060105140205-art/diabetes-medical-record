package filters;

import dal.DBContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/** Keeps one pooled connection per legacy request and always returns it to Hikari. */
@WebFilter("/*")
public class RequestConnectionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (path.startsWith("/static/") || path.startsWith("/uploads/")
                || path.equals("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }
        DBContext.beginRequest();
        try { chain.doFilter(request, response); }
        finally { DBContext.closeRequestConnections(); }
    }
}
