package NikKha03.UserService.component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OriginFilter implements Filter {

    @Value("${app-env.gateway-secret}")
    private String gatewaySecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (!gatewaySecret.equals(httpRequest.getHeader("X-Gateway-Secret"))) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("Access denied: missing or invalid Gateway header");
            return;
        }

        chain.doFilter(request, response); // Пропускаем запрос
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
