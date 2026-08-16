// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa un partido del calendario tal como lo
//        entrega una API externa (API-Football / football-data.org / Soccerway #4).
// [POR QUÉ]: Aísla el formato externo de la fuente del aggregate Partido. El caso
//            de uso CU-02 mapea este DTO al dominio (resolviendo los Equipo por nombre
//            y llevando la jornada).
// [ALTERNATIVAS]: Que el ProveedorCalendario devuelva List<Partido>; se descarta porque
//                 el proveedor no conoce los ids de los Equipo del dominio.
// [RELACIONES]: Devuelto por ProveedorCalendario; consumido por CU-02.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import java.time.LocalDateTime;

public record PartidoFuente(
        String equipoLocalNombre,
        String equipoVisitanteNombre,
        LocalDateTime fechaHora,
        Integer jornada) {
}