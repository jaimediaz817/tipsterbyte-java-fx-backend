package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultadoTest {

    @Test
    void debe_aceptar_marcador_valido() {
        assertDoesNotThrow(() -> new Resultado(2, 1));
    }

    @Test
    void debe_aceptar_cero_a_cero() {
        assertDoesNotThrow(() -> new Resultado(0, 0));
    }

    @Test
    void debe_rechazar_goles_negativos() {
        assertThrows(DomainException.class, () -> new Resultado(-1, 0));
    }
}