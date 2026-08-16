// ─────────────────────────────────────────────
// [QUÉ]: Excepción de infraestructura para errores de adapters externos (HTTP, Redis,
//        serialización, BD corrupta) que no son reglas de negocio pero tampoco deben
//        llegar al cliente como 500 genérico.
// [POR QUÉ]: Separa errores operativos (red caída, scraper no responde, cache roto)
//            de errores de negocio (DomainException → 422) y de bugs genuinos (500).
//            El GlobalExceptionHandler la traduce a 503 Service Unavailable.
// [ALTERNATIVAS]: Reutilizar DomainException; se descarta porque un error de red no
//                 es una regla de negocio. Usar RuntimeException crudo; se descarta
//                 porque llegaría como 500 sin contexto.
// [RELACIONES]: Lanzada por infrastructure.adapter; capturada por GlobalExceptionHandler.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.exception;

public class InfraestructureException extends RuntimeException {

    public InfraestructureException(String message) {
        super(message);
    }

    public InfraestructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
