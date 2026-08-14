// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa la fecha y hora programada de un partido.
// [POR QUÉ]: Encapsula el momento del partido con su validación de nulidad y
//            futuro, de forma reutilizable y sin dispersar la lógica.
// [ALTERNATIVAS]: LocalDateTime suelto; se descarta porque perdería el control
//                 de cuándo es válida una fecha de programación.
// [RELACIONES]: Usado por el aggregate Partido (CU-02).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.time.LocalDateTime;

public record FechaProgramada(LocalDateTime fechaHora) {

    // [QUÉ]: Compact constructor que rechaza fechas nulas.
    public FechaProgramada {
        if (fechaHora == null) {
            throw new DomainException("Fecha programada no puede ser nula");
        }
    }

    // [QUÉ]: Indica si la fecha ya llegó (se usa para decidir si el partido inicia).
    public boolean esPasada(LocalDateTime momento) {
        return momento != null && !fechaHora.isAfter(momento);
    }
}