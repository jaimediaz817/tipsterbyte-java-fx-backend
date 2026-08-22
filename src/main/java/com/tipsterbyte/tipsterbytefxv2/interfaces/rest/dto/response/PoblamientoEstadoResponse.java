// ─────────────────────────────────────────────
// [QUÉ]: Response DTO del polling de poblamiento manual (FASE T3).
// [POR QUÉ]: Alimenta la barra de progreso del frontend: estado RUNNING/SUCCESS/ERROR,
//            país en curso, países procesados y duración. paisActual/paisesProcesados
//            solo tienen valor mientras RUNNING (snapshot en memoria).
// [RELACIONES]: CatalogoController GET /catalogo/activar/{executionId} → TareaLog +
//               ProgresoPoblamiento.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record PoblamientoEstadoResponse(
        String executionId,
        String estado,
        String paisActual,
        Integer paisesProcesados,
        String fechaInicio,
        Long duracionMs,
        String mensaje) {
}
