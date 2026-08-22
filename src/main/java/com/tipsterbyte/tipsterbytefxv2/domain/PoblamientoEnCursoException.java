// ─────────────────────────────────────────────
// [QUÉ]: Excepción de dominio que indica que ya existe una ejecución de poblamiento
//        geográfico en curso (anti-solapamiento de la vía manual).
// [POR QUÉ]: Lanzar un segundo poblamiento mientras corre otro duplicaría carga sobre
//            el scraper y mezclaría resultados. Se mapea a HTTP 409 Conflict para que
//            el frontend distinga "duplicado" de "error" (422).
// [ALTERNATIVAS]: Reutilizar DomainException; se descarta porque se mapea a 422 y
//                 semánticamente NO es una violación de regla de negocio sino de
//                 concurrencia.
// [RELACIONES]: Lanzada por SincronizarCatalogoAsyncUseCase (FASE T3); mapeada a 409
//               por GlobalExceptionHandler.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain;

public class PoblamientoEnCursoException extends RuntimeException {

    public PoblamientoEnCursoException(String mensaje) {
        super(mensaje);
    }
}
