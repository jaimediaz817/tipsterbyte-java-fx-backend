package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CuotaTest {

    @Test
    void debe_aceptar_cuota_mayor_a_uno() {
        Cuota cuota = new Cuota(new BigDecimal("1.85"));
        assertEquals(new BigDecimal("1.85"), cuota.valor());
        // Constructor sin mercado: asume UNO_X_DOS (compatibilidad).
        assertEquals(Mercado.UNO_X_DOS, cuota.mercado());
    }

    @Test
    void debe_preservar_el_mercado_de_la_cuota() {
        Cuota cuota = new Cuota(Mercado.DOBLE_OPORTUNIDAD, new BigDecimal("1.45"));
        assertEquals(Mercado.DOBLE_OPORTUNIDAD, cuota.mercado());
        assertEquals(new BigDecimal("1.45"), cuota.valor());
    }

    @Test
    void debe_rechazar_mercado_nulo() {
        assertThrows(DomainException.class, () -> new Cuota(null, new BigDecimal("1.85")));
    }

    @Test
    void debe_rechazar_cuota_igual_a_uno() {
        assertThrows(DomainException.class, () -> new Cuota(BigDecimal.ONE));
    }

    @Test
    void debe_rechazar_cuota_menor_a_uno() {
        assertThrows(DomainException.class, () -> new Cuota(new BigDecimal("0.95")));
    }

    @Test
    void debe_rechazar_cuota_nula() {
        assertThrows(DomainException.class, () -> new Cuota(null));
    }
}