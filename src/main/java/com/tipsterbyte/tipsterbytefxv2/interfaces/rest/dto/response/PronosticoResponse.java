// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de un pronóstico público consultado por un cliente (CU-08).
//        Copia la forma de PronosticoPublicoDto de application a la capa interfaces
//        para mantener la Dependency Rule (interfaces no expone application.dto
//        directamente al cliente HTTP).
// [POR QUÉ]: La capa interfaces debe definir su propio contrato de respuesta; aunque
//            hoy PronosticoPublicoDto vive en application, exponerlo desde el
//            controller acopla el contrato HTTP a la capa application. Este DTO
//            desacopla y permite evolucionar la vista REST sin tocar application.
// [ALTERNATIVAS]: Reutilizar PronosticoPublicoDto; se descarta porque viola la
//                 separación de capas (interfaces → application → domain).
// [RELACIONES]: PronosticoController GET /api/v1/pronosticos → PronosticoResponse[].
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PronosticoResponse(
        UUID pronosticoId,
        UUID tipsterId,
        UUID partidoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        Mercado mercado,
        String resultadoEsperado,
        BigDecimal cuotaValor) {
}
