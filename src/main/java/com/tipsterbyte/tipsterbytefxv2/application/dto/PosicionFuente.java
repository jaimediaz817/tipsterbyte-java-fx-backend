// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa una fila de la tabla de posiciones tal como
//        la entrega una API externa (football-data.org / API-Football).
// [POR QUÉ]: Aísla el formato externo de la fuente del VO de dominio PosicionTabla.
//            El caso de uso CU-01 mapea este DTO al VO (buscando el Equipo en la liga).
// [ALTERNATIVAS]: Que el ProveedorPosiciones devuelva directamente List<PosicionTabla>;
//                 se descarta porque el proveedor no conoce las entidades Equipo del dominio.
// [RELACIONES]: Devuelto por ProveedorPosiciones; consumido por CU-01.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record PosicionFuente(
        String equipoNombre,
        int posicion,
        int jugados,
        int ganados,
        int empatados,
        int perdidos,
        int golesFavor,
        int golesContra,
        int puntos) {
}