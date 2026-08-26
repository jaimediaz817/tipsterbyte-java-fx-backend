// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de CU-16 (sincronizar equipos de una liga desde la fuente #6):
//        conteo de creados/actualizados, total de la plantilla y bandera que indica
//        si la respuesta vino de la plantilla existente (sin consultar la fuente).
// [POR QUÉ]: Alimenta los badges de la pantalla "Países de interés → Ligas de mis
//            países" (ej: "28/30") y permite al frontend distinguir "ya tenía N
//            equipos, no se re-scrapeó" de "scrape real ejecutado" (HU-FRONT-05).
// [ALTERNATIVAS]: Reutilizar SincronizacionResponse (eventosEmitidos); se descarta porque
//                 el usuario quiere conteos de plantilla, no eventos internos.
// [RELACIONES]: LigaController POST /{ligaId}/equipos/sincronizar → CU-16.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record EquiposSincronizadosResponse(
        int creados,
        int actualizados,
        int totalEquipos,
        boolean desdePlantillaExistente) {
}
