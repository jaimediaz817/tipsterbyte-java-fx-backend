// ─────────────────────────────────────────────
// [QUÉ]: Manejador global de excepciones REST (@RestControllerAdvice) que traduce
//        excepciones de dominio y de validación a respuestas HTTP con ApiError.
// [POR QUÉ]: Centraliza el mapeo error→status: DomainException (reglas BR-xx) → 422
//            Unprocessable Content; validación de la request → 400 con detalles por
//            campo; JSON malformado o enum inválido → 400; resto → 500.
// [ALTERNATIVAS]: try/catch en cada controller; se descarta por duplicar lógica y
//                 mezclar el contrato de error con la lógica del endpoint.
// [RELACIONES]: Captura DomainException (domain) y excepciones de Spring Validation/HTTP.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // [QUÉ]: Traduce una violación de regla de negocio (DomainException, BR-xx) a 422.
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> manejarDomainException(DomainException ex, HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
    }

    // [QUÉ]: Traduce errores de validación de un @Valid request body a 400 con el
    //        mensaje del primer campo inválido.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "Request inválida: " + detalles, request);
    }

    // [QUÉ]: Traduce JSON malformado, tipos incorrectos o enums inválidos a 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarBodyNoLegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Cuerpo de la request inválido o malformado", request);
    }

    // [QUÉ]: Traduce un parámetro de consulta (query param) obligatorio ausente a 400.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> manejarParametroFaltante(MissingServletRequestParameterException ex,
                                                             HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Parámetro obligatorio ausente: " + ex.getParameterName(), request);
    }

    // [QUÉ]: Traduce cualquier otra excepción no esperada a 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarGeneral(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    // [QUÉ]: Construye la respuesta ApiError con status, mensaje y ruta de la request.
    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}