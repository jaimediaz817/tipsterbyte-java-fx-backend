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