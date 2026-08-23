// ─────────────────────────────────────────────
// [QUÉ]: Response DTO del diagnóstico de discrepancias (H-04): pares sospechosos de
//        duplicado en la plantilla de una liga.
// [POR QUÉ]: Alimenta el panel de revisión del admin. La detección es SOLO informativa:
//            el sistema nunca fusiona ni elimina automáticamente.
// [RELACIONES]: GET /api/v1/ligas/{ligaId}/equipos/discrepancias → CU H-04.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.util.List;
import java.util.UUID;

public record DiscrepanciasResponse(
        UUID ligaId,
        UUID temporadaId,
        String temporadaNombre,
        int totalPares,
        List<ParResponse> pares) {

    public record ParResponse(
            EquipoResponse equipoA,
            EquipoResponse equipoB,
            String razon) {
    }
}
