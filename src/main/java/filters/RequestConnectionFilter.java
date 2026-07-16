package filters;

import dal.DBContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

/** Keeps one pooled connection per legacy request and always returns it to Hikari. */
@WebFilter("/*")
public class RequestConnectionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        DBContext.beginRequest();
        try { chain.doFilter(request, response); }
        finally { DBContext.closeRequestConnections(); }
    }
}
