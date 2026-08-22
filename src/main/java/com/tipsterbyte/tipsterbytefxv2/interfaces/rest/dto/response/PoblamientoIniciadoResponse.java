// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de POST /catalogo/activar (FASE T3): id de la ejecución lanzada.
// [POR QUÉ]: El poblamiento pasó a asíncrono: el frontend usa executionId para hacer
//            polling del estado vía GET /catalogo/activar/{executionId}.
// [RELACIONES]: CatalogoController → SincronizarCatalogoAsyncUseCase (H-02).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record PoblamientoIniciadoResponse(
        String executionId,
        String estado,
        String urlEstado) {
}
