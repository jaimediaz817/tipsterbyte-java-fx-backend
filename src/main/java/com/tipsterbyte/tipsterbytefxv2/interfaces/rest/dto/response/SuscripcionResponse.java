// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de CU-09 (crear suscripción): la vista del recurso creado con
//        su id, actor, plan y periodo de vigencia.
// [POR QUÉ]: Devuelve al cliente el resultado de la operación sin exponer el aggregate
//            Suscripcion (con sus eventos internos). Se construye a partir del id
//            generado y los datos de la request.
// [ALTERNATIVAS]: Devolver el aggregate; se descarta por acoplar interfaces al dominio.
// [RELACIONES]: CU-09 → SuscripcionResponse (interfaces.rest).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuscripcionResponse(
        UUID suscripcionId,
        UUID clienteId,
        UUID tipsterId,
        String planNombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoSuscripcion estado) {
}