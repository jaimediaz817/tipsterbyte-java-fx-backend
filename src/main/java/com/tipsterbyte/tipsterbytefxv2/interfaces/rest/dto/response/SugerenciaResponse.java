// ─────────────────────────────────────────────
// [QUÉ]: DTO de respuesta para un pronóstico sugerido (HU-16 AC13).
// [POR QUÉ]: El frontend muestra las sugerencias con datos del partido + score.
// [RELACIONES]: ConsultarSugerenciasUseCase → SugerenciaResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;

public record SugerenciaResponse(
        UUID sugerenciaId,
        UUID estrategiaId,
        UUID partidoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaPartido,
        Integer jornada,
        BigDecimal score,
        int criteriosCumplidos,
        int criteriosFallidos,
        Instant createdAt) {
}
