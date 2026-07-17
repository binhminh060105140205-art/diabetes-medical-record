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
            httpResponse.setHeader("Cache-Control", "public, max-age=604800, immutable");
        } else {
            httpResponse.setHeader("Cache-Control", "public, max-age=600, stale-while-revalidate=86400");
        }
        chain.doFilter(request, response);
    }
}
