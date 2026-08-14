package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanTest {

    @Test
    void debe_aceptar_plan_valido() {
        assertDoesNotThrow(() -> new Plan("Premium", new BigDecimal("9.99"), 30));
    }

    @Test
    void debe_rechazar_plan_con_nombre_vacio() {
        assertThrows(DomainException.class, () -> new Plan(" ", new BigDecimal("9.99"), 30));
    }

    @Test
    void debe_rechazar_plan_con_precio_negativo() {
        assertThrows(DomainException.class, () -> new Plan("Premium", new BigDecimal("-1"), 30));
    }

    @Test
    void debe_rechazar_plan_con_duracion_no_positiva() {
        assertThrows(DomainException.class, () -> new Plan("Premium", new BigDecimal("9.99"), 0));
    }
}