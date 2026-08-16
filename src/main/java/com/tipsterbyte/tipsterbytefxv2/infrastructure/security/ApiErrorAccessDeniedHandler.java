// ─────────────────────────────────────────────
// [QUÉ]: AccessDeniedHandler custom que devuelve ApiError en formato JSON para todas
//        las respuestas 403 (autenticado pero sin permisos suficientes).
// [POR QUÉ]: Spring Security por defecto responde 403 con HTML o texto plano. Este
//            handler garantiza que el frontend reciba siempre el mismo schema de
//            error (ApiError) sin importar si la excepción viene de dominio o de
//            la cadena de filtros de seguridad.
// [ALTERNATIVAS]: Dejar el default de Spring Security; se descarta porque rompe el
//                 contrato JSON de la API.
// [RELACIONES]: SecurityConfig → ApiErrorAccessDeniedHandler; usa ApiError.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class ApiErrorAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiErrorAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Acceso denegado: el rol actual no tiene permiso para este recurso",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
