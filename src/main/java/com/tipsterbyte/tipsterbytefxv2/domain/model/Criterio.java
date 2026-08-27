// ─────────────────────────────────────────────
// [QUÉ]: VO embebido que representa un criterio individual dentro de una estrategia.
// [POR QUÉ]: Cada criterio se auto-describe con fuente, campo, operador, valor,
//            referencia y peso. Se modela como embeddable (no entity independiente)
//            porque su identidad vive dentro de la estrategia padre.
// [ALTERNATIVAS]: Entity separada con FK; se descarta porque el criterio no existe
//                 fuera de la estrategia y no tiene lifecycle propio.
// [RELACIONES]: Compone `Estrategia` (AC2 HU-16).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class Criterio {

    private final FuenteCriterio fuente;
    private final String campo;
    private final OperadorCriterio operador;
    private final String valor;
    private final ReferenciaCriterio referencia;
    private final BigDecimal peso;
    private final Integer orden;

    public Criterio(FuenteCriterio fuente, String campo, OperadorCriterio operador,
                    String valor, ReferenciaCriterio referencia, BigDecimal peso, Integer orden) {
        if (fuente == null) throw new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Criterio requiere fuente");
        if (campo == null || campo.isBlank()) throw new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Criterio requiere campo");
        if (operador == null) throw new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Criterio requiere operador");
        if (referencia == null) throw new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Criterio requiere referencia");
        if (peso == null || peso.compareTo(BigDecimal.ZERO) < 0 || peso.compareTo(BigDecimal.ONE) > 0) {
            throw new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Criterio requiere peso entre 0 y 1");
        }
        this.fuente = fuente;
        this.campo = campo;
        this.operador = operador;
        this.valor = valor;
        this.referencia = referencia;
        this.peso = peso;
        this.orden = orden;
    }

    public FuenteCriterio fuente() { return fuente; }
    public String campo() { return campo; }
    public OperadorCriterio operador() { return operador; }
    public String valor() { return valor; }
    public ReferenciaCriterio referencia() { return referencia; }
    public BigDecimal peso() { return peso; }
    public Integer orden() { return orden; }

    public enum FuenteCriterio {
        CUOTAS, POSICIONES, FORMA, ZONA_DESCENSO
    }

    public enum OperadorCriterio {
        MAYOR_IGUAL, MENOR_IGUAL, IGUAL, MAYOR, MENOR, CONTIENE, NO_CONTIENE
    }

    public enum ReferenciaCriterio {
        LOCAL, VISITANTE, AMBOS
    }
}
