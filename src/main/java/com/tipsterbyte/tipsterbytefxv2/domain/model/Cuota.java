// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa el multiplicador de una apuesta para un
//        mercado (ej: 1.85 para "local gana" en el mercado 1X2).
// [POR QUÉ]: Fija el patrón de VO del proyecto: inmutable, validación en el
//            constructor y sin setters. En el dominio, una cuota se define por su
//            mercado y su valor. Añadir Mercado (FASE 8.5) es necesario para
//            persistir las cuotas reales de Wplay: 3 de 1X2 + 3 de doble oportunidad
//            por partido; sin el mercado la doble oportunidad se perdería en BD.
// [ALTERNATIVAS]: Solo BigDecimal (como antes); se descartó porque la fuente #2
//                 entrega cuotas de mercados distintos y el Pronostico valida la
//                 selección contra el mercado (CU-06).
// [RELACIONES]: Usada por el aggregate Partido (CU-03) y Pronostico (CU-06). Violación → DomainException.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.math.BigDecimal;

public final class Cuota {

    private final Mercado mercado;
    private final BigDecimal valor;

    // [QUÉ]: Construye una cuota con su mercado, validando BR-007 (valor > 1.0).
    // [POR QUÉ]: Toda cuota pertenece a un mercado; se exige no nulo para que el
    //            pronóstico (CU-06) y la persistencia (CuotaEntity) conozcan el mercado.
    public Cuota(Mercado mercado, BigDecimal valor) {
        if (mercado == null) {
            throw new DomainException("Cuota requiere mercado");
        }
        if (valor == null || valor.compareTo(BigDecimal.ONE) <= 0) {
            throw new DomainException("Cuota debe ser mayor que 1.0 (BR-007)");
        }
        this.mercado = mercado;
        this.valor = valor;
    }

    // [QUÉ]: Constructor compatible con usos previos (PRONOSTICO manual o tests):
    //        asume mercado UNO_X_DOS.
    // [POR QUÉ]: No romper contratos existentes que solo expresan un valor de cuota
    //            (ej: CrearPronosticoUseCase lo sustituirá por el mercado de la selección).
    // [ALTERNATIVAS]: Forzar siempre el mercado; se descartó por compatibilidad, aunque
    //                 los casos de uso nuevos (CU-03) siempre pasan el mercado real.
    public Cuota(BigDecimal valor) {
        this(Mercado.UNO_X_DOS, valor);
    }

    public Mercado mercado() {
        return mercado;
    }

    // [QUÉ]: Expone el valor de la cuota de forma inmutable (sin setter).
    public BigDecimal valor() {
        return valor;
    }
}