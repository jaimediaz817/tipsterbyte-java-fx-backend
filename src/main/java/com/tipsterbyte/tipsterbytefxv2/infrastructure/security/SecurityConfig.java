// ─────────────────────────────────────────────
// [QUÉ]: Configuración de Spring Security (FASE 11): cadena de filtros stateless,
//        endpoints públicos vs autenticados, autorización por rol y 401/403.
// [POR QUÉ]: Centraliza la política de seguridad. /api/v1/auth/** (registro/login) y
//            /actuator/health son públicos; el resto exige JWT válido. Los roles
//            TIPSTER/CLIENTE/ADMIN se mapean como ROLE_<ROL> (hasRole). CSRF se
//            desactiva porque la API es stateless con JWT (sin cookies de sesión).
// [ALTERNATIVAS]: Sesiones con cookies (csrf activo); se descarta porque el frontend
//                 Angular consume API stateless. Seguridad por método (@PreAuthorize);
//                 se deja para granularidad futura, aquí se define por path.
// [RELACIONES]: Registra JwtAuthenticationFilter; asegura todos los controllers
//               interfaces.rest.controller (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // [QUÉ]: Construye la configuración con el filtro JWT.
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // [QUÉ]: Define la cadena de filtros de seguridad (stateless + JWT + roles).
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fuentes", "/api/v1/fuentes/**").hasAnyRole("ADMIN", "TIPSTER", "CLIENTE")
                        .requestMatchers("/api/v1/ligas/**").hasAnyRole("ADMIN", "TIPSTER")
                        .requestMatchers("/api/v1/partidos/**").hasAnyRole("ADMIN", "TIPSTER")
                        .requestMatchers("/api/v1/pronosticos/**").hasAnyRole("ADMIN", "TIPSTER", "CLIENTE")
                        .requestMatchers("/api/v1/suscripciones/**").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                        // [POR QUÉ]: Sin token (o inválido) → 401; con token pero sin rol → 403.
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
