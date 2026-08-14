package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeleccionPronosticoTest {

    @Test
    void debe_aceptar_seleccion_1x2_valida() {
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.UNO_X_DOS, "1"));
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.UNO_X_DOS, "X"));
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.UNO_X_DOS, "2"));
    }

    @Test
    void debe_rechazar_seleccion_invalida_para_1x2() {
        assertThrows(DomainException.class, () -> new SeleccionPronostico(Mercado.UNO_X_DOS, "12"));
    }

    @Test
    void debe_aceptar_seleccion_doble_oportunidad_valida() {
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.DOBLE_OPORTUNIDAD, "1X"));
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.DOBLE_OPORTUNIDAD, "12"));
        assertDoesNotThrow(() -> new SeleccionPronostico(Mercado.DOBLE_OPORTUNIDAD, "X2"));
    }

    @Test
    void debe_rechazar_seleccion_invalida_para_over_under() {
        assertThrows(DomainException.class, () -> new SeleccionPronostico(Mercado.OVER_UNDER, "2"));
    }

    @Test
    void debe_rechazar_resultado_esperado_vacio() {
        assertThrows(DomainException.class, () -> new SeleccionPronostico(Mercado.UNO_X_DOS, " "));
    }
}