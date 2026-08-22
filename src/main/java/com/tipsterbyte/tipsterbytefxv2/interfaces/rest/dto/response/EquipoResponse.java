// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de un equipo (H-03): identidad + nombre + escudo.
// [POR QUÉ]: El frontend pinta la plantilla poblada por la fuente #6 con escudos
//            (`equipos.logo_url`). Reutilizable en futuras pantallas (detalle de liga).
// [RELACIONES]: GET /api/v1/ligas/{ligaId}/equipos → EquiposLigaResponse.equipos[].
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.util.UUID;

public record EquipoResponse(
        UUID id,
        String nombre,
        String logoUrl) {
}
