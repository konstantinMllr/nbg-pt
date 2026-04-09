package nbgpt.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nbgpt.service.IpBlockingService;
import nbgpt.service.LlamaClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
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
        String ip = request.getRemoteAddr();

        if (ipBlockingService.isBlocked(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Ihre IP wurde wegen unangemessenem Verhalten blockiert.");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(ip, null, java.util.Collections.emptyList())
        );
        filterChain.doFilter(request, response);
    }
}
