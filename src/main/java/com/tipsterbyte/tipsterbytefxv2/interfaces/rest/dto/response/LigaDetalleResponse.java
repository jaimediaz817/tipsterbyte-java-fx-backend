// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de detalle de liga: incluye los datos básicos más la tabla
//        de posiciones completa (HU-01).
// [POR QUÉ]: El frontend necesita una vista de detalle que muestre la liga y su
//            clasificación en una sola pantalla, evitando múltiples requests.
// [ALTERNATIVAS]: Dos endpoints separados (liga + posiciones); se descarta porque
//                 el usuario tipster típicamente quiere ambos datos juntos.
// [RELACIONES]: LigaController GET /api/v1/ligas/{ligaId} → LigaRepository.buscarPorId().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;

import java.util.List;
import java.util.UUID;

public record LigaDetalleResponse(
        UUID id,
        String nombre,
        String pais,
        EstadoLiga estado,
        String temporada,
        List<PosicionTablaResponse> posiciones) {
}
