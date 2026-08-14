// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa la selección de un pronóstico: el mercado
//        elegido y el resultado esperado dentro de ese mercado.
// [POR QUÉ]: La selección solo tiene sentido dentro de su mercado. Este VO ata
//            ambos conceptos y valida que la selección sea coherente con el
//            mercado elegido (ej: en 1X2 solo "1", "X" o "2").
// [ALTERNATIVAS]: Dos campos sueltos; se descarta porque permitiría selecciones
//                 imposibles (ej: "1" en un mercado Over/Under).
// [RELACIONES]: Usada por el aggregate Pronostico (CU-06, CU-07).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

public final class SeleccionPronostico {

    private final Mercado mercado;
    private final String resultadoEsperado;

    // [QUÉ]: Construye la selección validando la coherencia con el mercado.
    public SeleccionPronostico(Mercado mercado, String resultadoEsperado) {
        if (mercado == null) {
            throw new DomainException("Selección requiere un mercado");
        }
        if (resultadoEsperado == null || resultadoEsperado.isBlank()) {
            throw new DomainException("Selección requiere un resultado esperado");
        }
        validarContraMercado(mercado, resultadoEsperado);
        this.mercado = mercado;
        this.resultadoEsperado = resultadoEsperado;
    }

    // [QUÉ]: Valida que el resultado esperado sea válido para el mercado elegido.
    // [POR QUÉ]: Regla de coherencia del negocio: cada mercado acepta solo sus resultados.
    private void validarContraMercado(Mercado mercado, String resultadoEsperado) {
        switch (mercado) {
            case UNO_X_DOS -> {
                if (!resultadoEsperado.matches("1|X|2")) {
                    throw new DomainException("Mercado 1X2 solo admite 1, X o 2");
                }
            }
            case DOBLE_OPORTUNIDAD -> {
                if (!resultadoEsperado.matches("1X|12|X2")) {
                    throw new DomainException("Doble oportunidad solo admite 1X, 12 o X2");
                }
            }
            case OVER_UNDER -> {
                if (!resultadoEsperado.matches("over|under")) {
                    throw new DomainException("Over/Under solo admite 'over' o 'under'");
                }
            }
        }
    }

    public Mercado mercado() {
        return mercado;
    }

    public String resultadoEsperado() {
        return resultadoEsperado;
    }
}