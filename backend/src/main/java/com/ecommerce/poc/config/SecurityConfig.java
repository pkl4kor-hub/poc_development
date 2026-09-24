package com.ecommerce.poc.config;

import com.ecommerce.poc.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SessionService sessionService;
    private final com.ecommerce.poc.repository.UserRepository userRepository;

    public SecurityConfig(SessionService sessionService, com.ecommerce.poc.repository.UserRepository userRepository) {
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
                    String sessionId = request.getHeader("X-Session-Id");
                    if (sessionId != null && !sessionId.isBlank()) {
                        String username = sessionService.resolveUsername(sessionId);
                        if (username != null) {
                            String role = "BUYER";
                            var userOpt = userRepository.findByUsername(username);
                            if (userOpt.isPresent()) {
                                role = userOpt.get().getRole().name();
                            }
                            var auth = new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(auth);
                            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
                        }
                    }
                    filterChain.doFilter(request, response);
                }
            }, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/auth/login"),
                    AntPathRequestMatcher.antMatcher("/api/auth/me"),
                    AntPathRequestMatcher.antMatcher("/api/products"),
                    AntPathRequestMatcher.antMatcher("/api/products/**"),
                    AntPathRequestMatcher.antMatcher("/api/chatbot/message"),
                    AntPathRequestMatcher.antMatcher("/api/health"),
                    AntPathRequestMatcher.antMatcher("/api/openapi.json"),
                    AntPathRequestMatcher.antMatcher("/api/config"),
                    AntPathRequestMatcher.antMatcher("/api/sessions"),
                    AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                    AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                    AntPathRequestMatcher.antMatcher("/swagger-ui.html"),
                    AntPathRequestMatcher.antMatcher("/images/**"),
                    AntPathRequestMatcher.antMatcher("/**")
                ).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/buyer/**")).hasRole("BUYER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/seller/**")).hasRole("SELLER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/admin/**")).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {});
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
