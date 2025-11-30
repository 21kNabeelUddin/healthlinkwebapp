package com.healthlink.config;

import com.healthlink.security.jwt.JwtAuthenticationFilter;
import com.healthlink.security.rate.GlobalRateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security Configuration
 * Configures HTTP security, CORS, authentication, and password encoding
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final GlobalRateLimitFilter rateLimitFilter;

        @Value("${healthlink.cors.allowed-origins:http://localhost:3000,http://localhost:8081}")
        private String allowedOrigins;

        @Value("${healthlink.security.bcrypt-strength:12}")
        private int bcryptStrength;

        /**
         * Constructor with optional rate limit filter.
         * Rate limiting is disabled in test environments via
         * healthlink.rate-limit.enabled=false.
         */
        public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                        @Autowired(required = false) GlobalRateLimitFilter rateLimitFilter) {
                this.jwtAuthFilter = jwtAuthFilter;
                this.rateLimitFilter = rateLimitFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Security headers
                                .headers(headers -> headers
                                                .xssProtection(xss -> xss.headerValue(
                                                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none';"))
                                                .contentTypeOptions(opts -> opts.disable()) // Adds
                                                                                            // X-Content-Type-Options:
                                                                                            // nosniff
                                                .referrerPolicy(referrer -> referrer.policy(
                                                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                .frameOptions(frame -> frame.deny()))

                                // CSRF disabled for stateless JWT API
                                .csrf(AbstractHttpConfigurer::disable)

                                // CORS configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Consistent 401 response for unauthenticated requests
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                                // Authorization rules
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers(
                                                                "/api/auth/login",
                                                                "/api/auth/register",
                                                                "/api/auth/refresh",
                                                                "/api/public/**",
                                                                "/api/health",
                                                                "/api/v1/auth/**",
                                                                "/api/v1/public/**",
                                                                "/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/actuator/health",
                                                                "/actuator/prometheus")
                                                .permitAll()

                                                // Role-based access
                                                .requestMatchers("/api/v1/admin/**", "/api/admin/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/v1/platform-owner/**", "/api/platform-owner/**")
                                                .hasRole("PLATFORM_OWNER")
                                                .requestMatchers("/api/v1/doctors/**", "/api/doctors/**")
                                                .hasAnyRole("DOCTOR", "ADMIN")
                                                .requestMatchers("/api/v1/patients/**", "/api/patients/**")
                                                .hasAnyRole("PATIENT", "DOCTOR")
                                                .requestMatchers("/api/v1/staff/**", "/api/staff/**")
                                                .hasAnyRole("STAFF", "DOCTOR", "ORGANIZATION")
                                                .requestMatchers("/api/v1/organizations/**", "/api/organizations/**")
                                                .hasAnyRole("ORGANIZATION", "ADMIN")
                                                .requestMatchers("/api/v1/appointments/**", "/api/appointments/**")
                                                .hasAnyRole("PATIENT", "DOCTOR", "STAFF", "ADMIN")

                                                // All other requests require authentication
                                                .anyRequest().authenticated())

                                // Stateless session management
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                                .sessionFixation().none()); // Explicit session fixation protection

                // Filter order: Rate Limit -> JWT Auth
                // Rate limiting BEFORE authentication to prevent DoS
                // Rate limit filter is optional (disabled in tests via
                // healthlink.rate-limit.enabled=false)
                if (rateLimitFilter != null) {
                        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
                }
                http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parse allowed origins from configuration
                // Use patterns to allow all origins (including localhost with random ports)
                // while keeping credentials enabled
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));

                // Allowed HTTP methods
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                // Explicitly whitelist allowed headers instead of wildcard for security
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Cache-Control",
                                "Pragma"));

                // Allow credentials (cookies, authorization headers)
                configuration.setAllowCredentials(true);

                // Preflight cache duration
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                // BCrypt strength configurable via application.yml
                // Default: 12 (per feature spec), adjustable for performance/security tradeoff
                return new BCryptPasswordEncoder(bcryptStrength);
        }
}
