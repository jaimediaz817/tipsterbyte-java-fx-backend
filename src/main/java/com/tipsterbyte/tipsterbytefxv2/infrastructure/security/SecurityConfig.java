// ─────────────────────────────────────────────
// [QUÉ]: Configuración de Spring Security (FASE 11): cadena de filtros stateless,
//        endpoints públicos vs autenticados, autorización por rol y 401/403.
// [POR QUÉ]: Centraliza la política de seguridad. /api/v1/auth/** (registro/login),
//            /api/v1/roles y /actuator/health son públicos; el resto exige JWT válido.
//            Los roles CLIENTE/TIPSTER/SUPERADMIN se mapean como ROLE_<ROL> (hasRole). CSRF se
//            desactiva porque la API es stateless con JWT (sin cookies de sesión).
// [ALTERNATIVAS]: Sesiones con cookies (csrf activo); se descarta porque el frontend
//                 Angular consume API stateless. Seguridad por método (@PreAuthorize);
//                 se deja para granularidad futura, aquí se define por path.
// [RELACIONES]: Registra JwtAuthenticationFilter; asegura todos los controllers
//               interfaces.rest.controller (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiErrorAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiErrorAccessDeniedHandler accessDeniedHandler;

    // [QUÉ]: Construye la configuración con el filtro JWT y los handlers de error.
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiErrorAuthenticationEntryPoint authenticationEntryPoint,
                          ApiErrorAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    // [QUÉ]: Define la cadena de filtros de seguridad (CORS + stateless + JWT + roles).
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/roles").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fuentes", "/api/v1/fuentes/**").hasAnyRole("SUPERADMIN", "TIPSTER", "CLIENTE")
                        .requestMatchers("/api/v1/ligas/**").hasAnyRole("SUPERADMIN", "TIPSTER")
                        .requestMatchers("/api/v1/paises/**").hasAnyRole("SUPERADMIN", "TIPSTER")
                        .requestMatchers("/api/v1/partidos/**").hasAnyRole("SUPERADMIN", "TIPSTER")
                        .requestMatchers("/api/v1/pronosticos/**").hasAnyRole("SUPERADMIN", "TIPSTER", "CLIENTE")
                        .requestMatchers("/api/v1/suscripciones/**").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // [QUÉ]: Configuración CORS para el frontend Angular en localhost:4200.
    // [POR QUÉ]: El frontend Angular (dev server) realiza peticiones cross-origin
    //            con credenciales (cookies/headers). Sin CORS explícito el navegador
    //            bloquea las respuestas aunque el backend devuelva 200.
    // [ALTERNATIVAS]: Proxy inverso (nginx) que unifique origen; se descarta porque
    //                 en desarrollo los servidores dev corren en puertos distintos.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
