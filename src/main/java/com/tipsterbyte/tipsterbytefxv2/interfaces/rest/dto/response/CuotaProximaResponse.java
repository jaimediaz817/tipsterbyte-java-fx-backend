// ─────────────────────────────────────────────
// [QUÉ]: DTO de respuesta para un partido en el snapshot de cuotas próximas (HU-15 AC1).
//        Incluye datos del partido + cuotas más recientes + volatilidad por mercado.
// [POR QUÉ]: El frontend renderiza una tabla/cards con la información más relevante
//            para que el tipster decida rápido qué partidos analizar.
// [RELACIONES]: ConsultarCuotasProximasUseCase → CuotaProximaResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.VolatilidadCuota;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CuotaProximaResponse(
        UUID partidoId,
        String equipoLocal,
        String equipoVisitante,
        Instant fechaPartido,
        Integer jornada,
        List<CuotaMercado> cuotas,
        VolatilidadCuota volatilidad) {

    public record CuotaMercado(
            String mercado,
            String seleccion,
            BigDecimal valor) {
    }
}
