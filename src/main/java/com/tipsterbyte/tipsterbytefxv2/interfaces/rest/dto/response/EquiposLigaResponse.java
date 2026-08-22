// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de la plantilla de equipos de una liga (H-03): temporada vigente
//        + listado completo con escudos.
// [POR QUÉ]: Cierra el ciclo visible de HU-11: el badge "28/30" del listado enlaza aquí
//            para ver QUIÉNES son los equipos. La temporada se expone para que el
//            frontend sepa qué plantilla está viendo (activa o primera registrada).
// [RELACIONES]: GET /api/v1/ligas/{ligaId}/equipos → EquipoResponse[].
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;

import java.util.List;
import java.util.UUID;

public record EquiposLigaResponse(
        UUID ligaId,
        UUID temporadaId,
        String temporadaNombre,
        EstadoTemporada temporadaEstado,
        int total,
        List<EquipoResponse> equipos) {
}
