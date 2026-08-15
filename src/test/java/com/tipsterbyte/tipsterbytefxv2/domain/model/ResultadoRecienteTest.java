package com.tipsterbyte.tipsterbytefxv2.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultadoRecienteTest {

    @Test
    void debe_mapear_codigos_de_la_fuente() {
        assertEquals(ResultadoReciente.GANADO, ResultadoReciente.desdeCodigo(1));
        assertEquals(ResultadoReciente.EMPATE, ResultadoReciente.desdeCodigo(0));
        assertEquals(ResultadoReciente.PERDIDO, ResultadoReciente.desdeCodigo(-1));
    }

    @Test
    void debe_rechazar_codigo_invalido() {
        assertThrows(IllegalArgumentException.class, () -> ResultadoReciente.desdeCodigo(5));
    }
}
