// ─────────────────────────────────────────────
// [QUÉ]: DTO de respuesta para CU-08 (consultar pronósticos por liga y fecha):
//        la vista pública de un pronóstico visible para el cliente.
// [POR QUÉ]: Aísla la capa de interfaces de los aggregates de dominio. Solo expone
//            datos PUBLICADOS (BR-006) y de tipsters suscritos, sin internals del
//            agregado Pronostico.
// [ALTERNATIVAS]: Devolver el aggregate Pronostico; se descarta porque exponería
//                 estado interno y acoplaría interfaces al dominio.
// [RELACIONES]: CU-08. Construido a partir de Pronostico + Partido + sus VOs.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PronosticoPublicoDto(
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