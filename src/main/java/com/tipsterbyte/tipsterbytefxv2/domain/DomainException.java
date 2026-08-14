// ─────────────────────────────────────────────
// [QUÉ]: Excepción base del dominio. Se lanza cuando se viola una regla de
//        negocio (BR-001..008) o una invariante de un value object/entity.
// [POR QUÉ]: Separa errores de negocio de errores de infraestructura. Extiende
//            RuntimeException (unchecked) para no forzar try/catch en casos de
//            uso; el exception handler global de interfaces la traduce a HTTP.
// [ALTERNATIVAS]: Checked exceptions (ensucian los casos de uso con manejo
//                 obligatorio); excepciones de Spring (acoplan dominio a framework).
// [RELACIONES]: Lanzada por domain.model; capturada por interfaces.rest.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain;

public class DomainException extends RuntimeException {

    // [QUÉ]: Construye la excepción con el mensaje de la regla violada.
    public DomainException(String message) {
        super(message);
    }

    // [QUÉ]: Construye la excepción con mensaje y causa raíz.
    // [POR QUÉ]: Permite encadenar errores sin perder la causa original.
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}