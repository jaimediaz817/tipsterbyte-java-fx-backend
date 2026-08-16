// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de consulta de partidos: vista resumida de un enfrentamiento
//        con equipos, fecha, jornada, estado, resultado y cuotas (HU-02, HU-03).
// [POR QUÉ]: El frontend necesita listados de partidos para el calendario y la
//            pantalla de pronósticos; la jornada alimenta el indicador cronológico
//            por liga y las cuotas se incluyen para decisiones rápidas.
// [ALTERNATIVAS]: DTO sin cuotas (requerir segundo endpoint); se descarta porque
//                 las cuotas son esenciales para el negocio de pronósticos.
// [RELACIONES]: PartidoController GET /api/v1/partidos; mapeado desde PartidoRepository
//               (CU-02, CU-03).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PartidoResponse(
        UUID id,
        UUID ligaId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaProgramada,
        Integer jornada,
        EstadoPartido estado,
        String resultado,
        List<CuotaResponse> cuotas) {
}
