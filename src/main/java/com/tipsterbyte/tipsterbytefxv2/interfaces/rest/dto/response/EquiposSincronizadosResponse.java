// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de CU-16 (sincronizar equipos de una liga desde la fuente #6):
//        conteo de equipos creados, actualizados y total de la plantilla resultante.
// [POR QUÉ]: Alimenta los badges de la pantalla "Países de interés → Ligas de mis
//            países" (ej: "28/30") y el feedback inmediato del botón "Poblar equipos".
// [ALTERNATIVAS]: Reutilizar SincronizacionResponse (eventosEmitidos); se descarta porque
//                 el usuario quiere conteos de plantilla, no eventos internos.
// [RELACIONES]: LigaController POST /{ligaId}/equipos/sincronizar → CU-16.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record EquiposSincronizadosResponse(
        int creados,
        int actualizados,
        int totalEquipos) {
}
