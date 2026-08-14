// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa el multiplicador de una apuesta para un
//        mercado (ej: 1.85 para "local gana").
// [POR QUÉ]: Fija el patrón de VO del proyecto: inmutable, validación en el
//            constructor y sin setters. En el dominio, una cuota se define por
//            su valor, no por una identidad.
// [ALTERNATIVAS]: Un simple BigDecimal en el aggregate Partido; se descarta porque
//                 una cuota tiene reglas (BR-007) y debe protegerse en sí misma.
// [RELACIONES]: Usada por el aggregate Partido (CU-03, CU-06). Violación → DomainException.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.math.BigDecimal;

public final class Cuota {

    private final BigDecimal valor;

    // [QUÉ]: Construye una cuota validando la regla BR-007 (valor > 1.0).
    // [POR QUÉ]: Un valor <= 1.0 no es una cuota válida en el negocio de apuestas.
    public Cuota(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ONE) <= 0) {
            throw new DomainException("Cuota debe ser mayor que 1.0 (BR-007)");
        }
        this.valor = valor;
    }

    // [QUÉ]: Expone el valor de la cuota de forma inmutable (sin setter).
    public BigDecimal valor() {
        return valor;
    }
}