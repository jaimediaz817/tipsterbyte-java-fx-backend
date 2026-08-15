// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa una fila de la tabla de posiciones tal como
//        la entrega una API externa (fuente #3 Flashscore: tabla_posiciones).
// [POR QUÉ]: Aísla el formato externo de la fuente del VO de dominio PosicionTabla.
//            El caso de uso CU-01 mapea este DTO al VO (buscando el Equipo en la liga).
//            Incluye la racha de últimos 5 resultados (1=G, 0=E, -1=P → GANADO/EMPATE/PERDIDO).
// [ALTERNATIVAS]: Que el ProveedorPosiciones devuelva directamente List<PosicionTabla>;
//                 se descarta porque el proveedor no conoce las entidades Equipo del dominio.
// [RELACIONES]: Devuelto por ProveedorPosiciones; consumido por CU-01.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;

import java.util.List;

public record PosicionFuente(
        String equipoNombre,
        int posicion,
        int jugados,
        int ganados,
        int empatados,
        int perdidos,
        int golesFavor,
        int golesContra,
        int puntos,
        List<ResultadoReciente> ultimosResultados) {

    // [QUÉ]: Constructor compatible con usos previos: racha vacía.
    public PosicionFuente(String equipoNombre, int posicion, int jugados, int ganados, int empatados,
                          int perdidos, int golesFavor, int golesContra, int puntos) {
        this(equipoNombre, posicion, jugados, ganados, empatados, perdidos,
                golesFavor, golesContra, puntos, List.of());
    }
}