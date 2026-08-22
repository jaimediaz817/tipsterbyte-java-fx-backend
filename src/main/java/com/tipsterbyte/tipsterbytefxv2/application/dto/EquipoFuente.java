// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa un equipo tal como lo entrega la fuente #6
//        (ext-soccerway-teams-by-league): nombre + escudo.
// [POR QUÉ]: Aísla el formato externo del entity Equipo del dominio. El consumidor
//            (CU-10 encadenado) decide el matching normalizado y el alta/reuso.
// [ALTERNATIVAS]: Que el proveedor devuelva entities de dominio; se descarta porque
//                 los puertos de fuente trabajan con DTOs aislados del dominio.
// [RELACIONES]: Devuelto por ProveedorEquiposPorLiga; consumido por CU-10 (HU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record EquipoFuente(
        String nombre,
        String logoUrl) {
}
