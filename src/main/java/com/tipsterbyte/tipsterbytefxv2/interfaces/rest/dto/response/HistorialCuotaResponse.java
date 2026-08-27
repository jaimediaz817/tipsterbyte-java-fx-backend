// ─────────────────────────────────────────────
// [QUÉ]: DTO de respuesta para el historial de cuotas de un partido (HU-15 AC2).
//        Devuelve la serie cronológica de capturas agrupada por mercado/selección.
// [POR QUÉ]: El frontend renderiza un gráfico/drill-down al expandir un partido.
// [RELACIONES]: ConsultarHistorialCuotasUseCase → HistorialCuotaResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record HistorialCuotaResponse(
        String mercado,
        String seleccion,
        List<Captura> capturas) {

    public record Captura(
            BigDecimal valor,
            Instant capturadaEn) {
    }
}
