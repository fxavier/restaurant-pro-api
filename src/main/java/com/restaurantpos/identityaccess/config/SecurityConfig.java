package com.restaurantpos.identityaccess.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.restaurantpos.identityaccess.logging.LoggingFilter;
import com.restaurantpos.identityaccess.security.JwtAccessDeniedHandler;
import com.restaurantpos.identityaccess.security.JwtAuthenticationEntryPoint;
import com.restaurantpos.identityaccess.security.RateLimitingFilter;
import com.restaurantpos.identityaccess.tenant.TenantContextFilter;

/**
 * Spring Security configuration for JWT-based authentication.
 * Configures:
 * - JWT authentication filter chain
 * - Password encoder (BCrypt with strength 12)
 * - CORS configuration
 * - CSRF protection
 * - Authentication entry point and access denied handler
 * 
 * Requirements: 2.7, 13.6
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml",
        "/swagger-resources/**",
        "/webjars/**"
    };
    
    private final TenantContextFilter tenantContextFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final LoggingFilter loggingFilter;
    
    public SecurityConfig(TenantContextFilter tenantContextFilter, 
                         RateLimitingFilter rateLimitingFilter,
                         LoggingFilter loggingFilter) {
        this.tenantContextFilter = tenantContextFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.loggingFilter = loggingFilter;
    }
    
    /**
     * Configures the security filter chain with JWT authentication.
     * 
     * CSRF Protection Strategy:
     * - Enabled for all state-changing operations (POST, PUT, DELETE, PATCH)
     * - Disabled for /api/auth/** endpoints (stateless JWT authentication)
     * - Disabled for actuator endpoints (monitoring)
     * 
     * Requirement: 13.6
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure CSRF protection
            // Exclude stateless JWT authentication endpoints and monitoring endpoints
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/auth/**", "/actuator/**")
            )
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management (stateless for JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics", "/actuator/metrics/**").permitAll()
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers("/error").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Configure JWT authentication
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                .accessDeniedHandler(new JwtAccessDeniedHandler())
            )
            
            // Add rate limiting filter before authentication
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Add tenant context filter after JWT authentication
            .addFilterAfter(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Add logging filter after tenant context to ensure MDC is populated with tenant_id
            .addFilterAfter(loggingFilter, TenantContextFilter.class);
        
        return http.build();
    }

    /**
     * Bypass security filters entirely for Swagger/OpenAPI resources.
     * This avoids auth challenges on API docs static resources.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(SWAGGER_WHITELIST);
    }
    
    /**
     * Configures the password encoder with BCrypt and strength 12.
     * 
     * Requirement: 2.7
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * Configures CORS to allow cross-origin requests.
     * In production, this should be configured with specific allowed origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Configures JWT authentication converter to extract authorities from JWT claims.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
