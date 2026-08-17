// ─────────────────────────────────────────────
// [QUÉ]: DTO de aplicación que resume la jornada actual de una liga: la jornada del
//        próximo partido por jugarse y la jornada siguiente (ambas null si la liga no
//        tiene partidos con jornada).
// [POR QUÉ]: Aísla a los use cases de los response DTOs de interfaces y expresa la
//            jornada sin acoplar application a la capa HTTP.
// [ALTERNATIVAS]: Computar la jornada en el frontend; se descarta porque el backend
//                 es la única fuente de verdad (evita drift de reloj/cliente).
// [RELACIONES]: Producido por ObtenerJornadaActualUseCase (CU-02) y consumido por
//               LigaController (GET /api/v1/ligas/{id}/jornada-actual).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record JornadaActualDto(
        Integer jornadaActual,
        Integer proximaJornada) {
}