package nbgpt.config;
import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nbgpt.service.IpBlockingService;
import nbgpt.service.LlamaClient;
public class ContentModerationFilter extends OncePerRequestFilter {
    private final IpBlockingService ipBlockingService;
    private final LlamaClient llamaClient;
    public ContentModerationFilter(IpBlockingService ipBlockingService, LlamaClient llamaClient) {
        this.ipBlockingService = ipBlockingService;
        this.llamaClient = llamaClient;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (ipBlockingService.isBlocked(ip)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/event-stream;charset=UTF-8");
            response.getWriter().write("data: {\"text\":\"Ihre IP wurde wegen unangemessenem Verhalten blockiert.\"}\n\n");
            response.getWriter().flush();
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(ip, null, java.util.Collections.emptyList())
        );
        filterChain.doFilter(request, response);
    }
}
