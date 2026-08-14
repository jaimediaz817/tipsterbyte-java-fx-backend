package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosicionTablaTest {

    private static final Equipo EQUIPO = new Equipo("Equipo A");

    @Test
    void debe_aceptar_estadisticas_consistentes_br008() {
        assertDoesNotThrow(() -> new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10));
    }

    @Test
    void debe_rechazar_puntos_inconsistentes_br008() {
        assertThrows(DomainException.class,
                () -> new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 99));
    }

    @Test
    void debe_rechazar_jugados_inconsistentes_con_ganados_empatados_perdidos() {
        assertThrows(DomainException.class,
                () -> new PosicionTabla(EQUIPO, 1, 6, 3, 1, 1, 10, 4, 10));
    }

    @Test
    void debe_rechazar_posicion_menor_a_uno() {
        assertThrows(DomainException.class,
                () -> new PosicionTabla(EQUIPO, 0, 5, 3, 1, 1, 10, 4, 10));
    }
}