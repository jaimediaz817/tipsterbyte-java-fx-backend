// ─────────────────────────────────────────────
// [QUÉ]: DTO de error estándar devuelto por el GlobalExceptionHandler para cualquier
//        respuesta HTTP no 2xx: marca de tiempo, status, mensaje técnico y la ruta.
// [POR QUÉ]: Normaliza el formato de error de la API para que los clientes la
//            consuman de forma consistente y sin acoplarse a excepciones de Spring.
// [ALTERNATIVAS]: Respuestas de error ad-hoc por controller; se descarta porque
//                 complica el consumo y el debugging.
// [RELACIONES]: GlobalExceptionHandler (interfaces.rest.exception) → ApiError.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path) {
}