package com.tpi.paseadores.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.paseadores.shared.exception.ApiError;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     AuthenticationEntryPoint authenticationEntryPoint,
                                                     AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // .cors(cors -> cors.configurationSource(corsConfigurationSource())) // enable once the frontend is wired up
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /error must stay open: an unmapped path forwards here, and without this
                // a genuine 404 gets masked as 403 by the auth check below
                .requestMatchers("/api/auth/**", "/error").permitAll()
                // read-only catalog, no sensitive data: registration needs it before a token exists
                .requestMatchers(HttpMethod.GET, "/api/zones/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // missing/invalid token: same ApiError shape the rest of the API uses, instead of an empty body
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) ->
                writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, "Missing or invalid token", request.getRequestURI());
    }

    // authenticated but not allowed (e.g. wrong role): same ApiError shape too
    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) ->
                writeError(response, objectMapper, HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                                    HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // uncomment and wire into the filter chain above when the frontend needs cross-origin calls
    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    //     CorsConfiguration config = new CorsConfiguration();
    //     config.setAllowedOrigins(List.of("http://localhost:5173"));
    //     config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    //     config.setAllowedHeaders(List.of("*"));
    //     config.setAllowCredentials(true);
    //
    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/**", config);
    //     return source;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
