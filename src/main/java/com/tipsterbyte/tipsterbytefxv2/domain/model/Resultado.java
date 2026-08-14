// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa el marcador final de un partido.
// [POR QUÉ]: El resultado es un concepto atómico del negocio: goles locales y
//            visitantes juntos, con la regla de no admitir negativos (BR).
// [ALTERNATIVAS]: Dos ints sueltos; se descarta porque un marcador parcial
//                 (local sin visitante) no es un resultado válido.
// [RELACIONES]: Usado por el aggregate Partido cuando finaliza (CU-05, BR-003).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

public record Resultado(int golesLocal, int golesVisitante) {

    // [QUÉ]: Compact constructor que rechaza marcadores negativos.
    public Resultado {
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new DomainException("Resultado inválido: los goles no pueden ser negativos");
        }
    }
}