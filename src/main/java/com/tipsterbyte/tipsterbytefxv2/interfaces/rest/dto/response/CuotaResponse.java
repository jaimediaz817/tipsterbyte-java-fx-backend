// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de una cuota para un mercado específico (HU-03).
// [POR QUÉ]: El frontend necesita conocer el mercado (1X2, doble oportunidad, etc.)
//            además del valor numérico para mostrar opciones de apuesta correctamente.
// [ALTERNATIVAS]: Solo valor numérico; se descarta porque la fuente #2 (Wplay) entrega
//                 múltiples mercados y el frontend debe distinguirlos (FASE 8.5).
// [RELACIONES]: PartidoController GET /api/v1/partidos/{partidoId}/cuotas; embebido
//               en PartidoResponse.cuotas[]. Mapeado desde Partido.cuotas() (CU-03).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.math.BigDecimal;

public record CuotaResponse(
        String mercado,
        BigDecimal valor) {
}
