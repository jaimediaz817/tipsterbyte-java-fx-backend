// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa una temporada deportiva (año inicio/fin).
// [POR QUÉ]: Una temporada es un concepto del negocio con su propia regla:
//            el año de fin debe ser posterior al de inicio. Como VO, su igualdad
//            se define por sus valores, no por una identidad.
// [ALTERNATIVAS]: Dos ints sueltos en la entidad; se descarta porque la regla
//                 de rango quedaría dispersa y validable en cualquier lado.
// [RELACIONES]: Usado por el aggregate Liga (CU-02, CU-04).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

public record Temporada(int anioInicio, int anioFin) {

    // [QUÉ]: Compact constructor que valida que el año de fin sea posterior al de inicio.
    public Temporada {
        if (anioInicio <= 0 || anioFin <= 0) {
            throw new DomainException("Temporada con años inválidos");
        }
        if (anioFin <= anioInicio) {
            throw new DomainException("Temporada inválida: año de fin debe ser mayor al de inicio");
        }
    }
}