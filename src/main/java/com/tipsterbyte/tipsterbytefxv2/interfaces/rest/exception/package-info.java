/**
 * [QUÉ]: Manejo global de excepciones REST: traduce excepciones de dominio y de
 *        validación a respuestas HTTP con el DTO ApiError.
 * [POR QUÉ]: Centraliza el contrato de errores (DomainException → 422, validación/JSON
 *            inválido → 400, resto → 500) sin ensuciar los controllers.
 * [RELACIONES]: Captura DomainException (domain) y excepciones Spring; produce ApiError.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception;