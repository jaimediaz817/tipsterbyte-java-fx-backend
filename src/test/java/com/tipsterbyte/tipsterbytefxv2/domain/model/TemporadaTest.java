package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemporadaTest {

    @Test
    void debe_aceptar_temporada_con_fin_posterior() {
        assertDoesNotThrow(() -> new Temporada(2025, 2026));
    }

    @Test
    void debe_rechazar_temporada_con_fin_igual_al_inicio() {
        assertThrows(DomainException.class, () -> new Temporada(2025, 2025));
    }

    @Test
    void debe_rechazar_temporada_con_fin_anterior() {
        assertThrows(DomainException.class, () -> new Temporada(2026, 2025));
    }

    @Test
    void debe_rechazar_anios_no_positivos() {
        assertThrows(DomainException.class, () -> new Temporada(0, 2026));
    }
}