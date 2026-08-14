// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa una fila de la tabla de posiciones de una
//        liga: equipo, posición y todas las estadísticas acumuladas.
// [POR QUÉ]: Concentra la consistencia de las estadísticas en un solo artefacto.
//            Aplica BR-008 (puntos = 3*ganados + 1*empatados) y valida que los
//            partidos jugados coincidan con G+E+P.
// [ALTERNATIVAS]: Entidad persistida sin reglas; se descarta porque las posiciones
//                 se recalculan desde la fuente (CU-01) y su consistencia es regla
//                 de negocio (BR-008), no de almacenamiento.
// [RELACIONES]: Usada por el aggregate Liga (CU-01). Violación → DomainException.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

public record PosicionTabla(
        Equipo equipo,
        int posicion,
        int jugados,
        int ganados,
        int empatados,
        int perdidos,
        int golesFavor,
        int golesContra,
        int puntos) {

    // [QUÉ]: Compact constructor que valida la consistencia de las estadísticas (BR-008).
    public PosicionTabla {
        if (equipo == null) {
            throw new DomainException("Posición de tabla requiere un equipo");
        }
        if (posicion < 1) {
            throw new DomainException("Posición inválida: debe ser mayor o igual a 1");
        }
        if (ganados < 0 || empatados < 0 || perdidos < 0) {
            throw new DomainException("Estadísticas inválidas: no pueden ser negativas");
        }
        int partidosCalculados = ganados + empatados + perdidos;
        if (jugados != partidosCalculados) {
            throw new DomainException(
                    "Posiciones inconsistentes: jugados (" + jugados + ") debe ser G+E+P (" + partidosCalculados + ")");
        }
        int puntosCalculados = (3 * ganados) + (1 * empatados);
        if (puntos != puntosCalculados) {
            throw new DomainException(
                    "Posiciones inconsistentes (BR-008): puntos (" + puntos + ") debe ser 3*G+E (" + puntosCalculados + ")");
        }
    }
}