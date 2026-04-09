package nbgpt.config;

import nbgpt.service.IpBlockingService;
import nbgpt.service.LlamaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final IpBlockingService ipBlockingService;
    private final LlamaClient llamaClient;

    public SecurityConfig(IpBlockingService ipBlockingService, LlamaClient llamaClient) {
        this.ipBlockingService = ipBlockingService;
        this.llamaClient = llamaClient;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configure(http))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterBefore(new ContentModerationFilter(ipBlockingService, llamaClient), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

