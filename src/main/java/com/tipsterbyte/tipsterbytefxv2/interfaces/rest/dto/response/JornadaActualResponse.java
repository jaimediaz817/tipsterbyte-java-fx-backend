// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de la jornada actual de una liga: jornadaActual (jornada del
//        próximo partido por jugarse, o la del último jugado si el calendario terminó)
//        y proximaJornada (la siguiente). Ambos null si la liga no tiene partidos con
//        jornada sincronizada.
// [POR QUÉ]: El frontend muestra el indicador cronológico "Jornada X · temporada Y"
//            por liga; el cálculo vive en el backend (única fuente de verdad) y este
//            DTO expone el resultado sin acoplar interfaces a la lógica.
// [ALTERNATIVAS]: Incluir la jornada dentro de LigaDetalleResponse; se descarta porque
//                 es un dato que cambia con el tiempo y merece su propia consulta ligera.
// [RELACIONES]: LigaController (GET /api/v1/ligas/{id}/jornada-actual); mapeado desde
//               JornadaActualDto.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record JornadaActualResponse(
        Integer jornadaActual,
        Integer proximaJornada) {
}