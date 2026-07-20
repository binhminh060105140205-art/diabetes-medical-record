package filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Keeps CSS/JS in the browser between page transitions; versioned assets remain immutable. */
@WebFilter("/static/*")
public class StaticCacheFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String query = httpRequest.getQueryString();
        if (query != null && query.contains("v=")) {
            httpResponse.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else {
            // Revalidate unversioned assets so deployments never keep an outdated interface.
            httpResponse.setHeader("Cache-Control", "public, max-age=0, must-revalidate");
        }
        chain.doFilter(request, response);
    }
}
