// ─────────────────────────────────────────────
// [QUÉ]: AuthenticationEntryPoint custom que devuelve ApiError en formato JSON para
//        todas las respuestas 401 (no autenticado / token inválido o ausente).
// [POR QUÉ]: Spring Security por defecto responde 401 con HTML o un texto plano que
//            rompe el contrato JSON de la API. Este handler unifica el formato de
//            error con el GlobalExceptionHandler (mismo schema ApiError).
// [ALTERNATIVAS]: Dejar el default de Spring Security; se descarta porque el frontend
//                 Angular espera siempre JSON parseable.
// [RELACIONES]: SecurityConfig → ApiErrorAuthenticationEntryPoint; usa ApiError.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class ApiErrorAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiErrorAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "No autenticado: se requiere un token JWT válido",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
