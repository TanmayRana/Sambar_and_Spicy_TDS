package com.sambarspicy.config;

import com.sambarspicy.security.JwtAuthenticationFilter;
import com.sambarspicy.security.RestAccessDeniedHandler;
import com.sambarspicy.security.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/public/menu").permitAll()
                // Public authentication and resource endpoints
                .requestMatchers(
                    "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/csrf",
                    "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/auth/csrf",
                    "/error", "/uploads/**"
                ).permitAll()

                // Authenticated user session actions
                .requestMatchers(
                    "/api/auth/me", "/api/v1/auth/me"
                ).authenticated()

                // ADMIN ONLY MODULES
                .requestMatchers(
                    "/api/admin", "/api/admin/**",
                    "/api/v1/admin", "/api/v1/admin/**"
                ).hasRole("ADMIN")

                // BILLING MODULE (ADMIN + BILLING)
                .requestMatchers(HttpMethod.GET,
                    "/api/billing", "/api/billing/**", "/api/v1/billing", "/api/v1/billing/**",
                    "/api/bills", "/api/bills/**", "/api/v1/bills", "/api/v1/bills/**"
                ).hasAnyRole("ADMIN", "BILLING", "READ_ONLY")
                .requestMatchers(
                    "/api/billing", "/api/billing/**",
                    "/api/v1/billing", "/api/v1/billing/**",
                    "/api/bills", "/api/bills/**",
                    "/api/v1/bills", "/api/v1/bills/**"
                ).hasAnyRole("ADMIN", "BILLING")

                // INVENTORY MODULE: Read-Only permitted for READ_ONLY & ADMIN, writes strictly ADMIN
                .requestMatchers(HttpMethod.GET,
                    "/api/inventory", "/api/inventory/**",
                    "/api/v1/inventory", "/api/v1/inventory/**"
                ).hasAnyRole("ADMIN", "READ_ONLY")
                .requestMatchers(
                    "/api/inventory", "/api/inventory/**",
                    "/api/v1/inventory", "/api/v1/inventory/**"
                ).hasRole("ADMIN")

                // REPORTS & EXPENSES MODULE: Read-Only permitted for READ_ONLY & ADMIN, writes strictly ADMIN
                .requestMatchers(HttpMethod.GET,
                    "/api/reports", "/api/reports/**",
                    "/api/v1/reports", "/api/v1/reports/**",
                    "/api/operating-expenses", "/api/operating-expenses/**",
                    "/api/v1/operating-expenses", "/api/v1/operating-expenses/**"
                ).hasAnyRole("ADMIN", "READ_ONLY")
                .requestMatchers(
                    "/api/reports", "/api/reports/**",
                    "/api/v1/reports", "/api/v1/reports/**",
                    "/api/operating-expenses", "/api/operating-expenses/**",
                    "/api/v1/operating-expenses", "/api/v1/operating-expenses/**"
                ).hasRole("ADMIN")

                // NOTIFICATIONS MODULE: Read-Only permitted for READ_ONLY & ADMIN, writes strictly ADMIN
                .requestMatchers(HttpMethod.GET,
                    "/api/notifications", "/api/notifications/**",
                    "/api/v1/notifications", "/api/v1/notifications/**"
                ).hasAnyRole("ADMIN", "READ_ONLY")
                .requestMatchers(
                    "/api/notifications", "/api/notifications/**",
                    "/api/v1/notifications", "/api/v1/notifications/**"
                ).hasRole("ADMIN")

                // MENU / CATEGORIES / STORE: Read-Only for all 3 authenticated roles, writes strictly ADMIN
                .requestMatchers(HttpMethod.GET,
                    "/api/menu", "/api/menu/**", "/api/v1/menu", "/api/v1/menu/**",
                    "/api/menu-items", "/api/menu-items/**", "/api/v1/menu-items", "/api/v1/menu-items/**",
                    "/api/categories", "/api/categories/**", "/api/v1/categories", "/api/v1/categories/**",
                    "/api/store", "/api/store/**", "/api/v1/store", "/api/v1/store/**"
                ).hasAnyRole("ADMIN", "BILLING", "READ_ONLY")
                .requestMatchers(
                    "/api/menu", "/api/menu/**", "/api/v1/menu", "/api/v1/menu/**",
                    "/api/menu-items", "/api/menu-items/**", "/api/v1/menu-items", "/api/v1/menu-items/**",
                    "/api/categories", "/api/categories/**", "/api/v1/categories", "/api/v1/categories/**",
                    "/api/store", "/api/store/**", "/api/v1/store", "/api/v1/store/**"
                ).hasRole("ADMIN")

                // New API endpoints must receive an explicit authorization rule.
                .requestMatchers("/api/**", "/api/v1/**").denyAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Set-Cookie", "X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Filter that ensures CsrfToken value is rendered to populate the XSRF-TOKEN cookie on responses.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // Forces generation and sets cookie
            }
            filterChain.doFilter(request, response);
        }
    }
}
